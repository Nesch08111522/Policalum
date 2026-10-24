import { CommonModule } from '@angular/common';
import { Component, OnDestroy, effect, untracked, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink, Routes } from '@angular/router';
import { Subject, forkJoin, of, takeUntil } from 'rxjs';
import { catchError } from 'rxjs/operators';
import * as Api from '../../../core/api/openapi.types';
import { FacturacionSunatComponent } from '../../facturacion-electronica/components/facturacion-sunat.component';
import { TokenService } from '../../../services/token.service';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { CajaSessionService } from '../../../shared/services/caja-session.service';
import { SedeFiltroService } from '../../../shared/services/sede-filtro.service';
import { CatalogoFraccionesApiService } from '../../catalogo-fracciones/data-access/catalogo-fracciones.api';
import { ClientesApiService } from '../../clientes/data-access/clientes.api';
import { ComprasApiService } from '../../compras/data-access/compras.api';
import { CotizacionesApiService } from '../../cotizaciones/data-access/cotizaciones.api';
import { CuentasPorPagarApiService } from '../../cuentas-por-pagar/data-access/cuentas-por-pagar.api';
import { FinanzasApiService } from '../../finanzas/data-access/finanzas.api';
import { ReportesApiService } from '../../reportes/data-access/reportes.api';
import { StockApiService } from '../../stock/data-access/stock.api';
import { VentasApiService } from '../../ventas/data-access/ventas.api';

function iso(fecha: Date): string { return fecha.toISOString().slice(0, 10); }

interface FraccionPanel { nombre: string; etiqueta: string; ancho: number; }

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PenPipe, FacturacionSunatComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.css'],
})
export class DashboardPageComponent implements OnDestroy {
  readonly esAdmin: boolean;
  cargando = true;
  error: string | null = null;
  ventasHoy: Api.VentasResumenDTO | null = null;
  resumen: Api.VentasResumenDTO | null = null;
  stock: Api.StockDashboardDTO | null = null;
  cxp: Api.ResumenCxPDashboardDTO | null = null;
  kpis: Api.KpisFinancierosDTO | null = null;
  ingresos: number[] = [];
  gastos: number[] = [];
  etiquetas: string[] = [];
  sedeId = '';
  fracciones: FraccionPanel[] = [];
  productoFraccion = 'Sin fracciones activas';
  clientes = 0;
  cotizaciones = 0;
  private readonly destroy$ = new Subject<void>();
  private carga = 0;

  constructor(
    private readonly token: TokenService,
    private readonly reportes: ReportesApiService,
    private readonly stockApi: StockApiService,
    private readonly cxpApi: CuentasPorPagarApiService,
    private readonly finanzas: FinanzasApiService,
    private readonly ventasApi: VentasApiService,
    private readonly comprasApi: ComprasApiService,
    private readonly fraccionesApi: CatalogoFraccionesApiService,
    private readonly clientesApi: ClientesApiService,
    private readonly cotizacionesApi: CotizacionesApiService,
    private readonly sedeFiltro: SedeFiltroService,
    readonly caja: CajaSessionService,
  ) {
    this.esAdmin = this.token.getRol() === 'ROLE_ADMIN';
    effect(() => {
      const sedeId = this.sedeFiltro.sedeId();
      untracked(() => this.cargar(sedeId));
    });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  get cajaEstado(): string {
    const turno = this.caja.turno();
    if (!turno?.id) return 'Sin turno';
    return turno.estado === 'CERRADO' ? 'Cerrada' : 'Abierta';
  }

  get variacion(): string {
    const neto = Number(this.resumen?.neto ?? 0);
    const total = Number(this.resumen?.total ?? 0);
    if (!total) return '0%';
    return `${((neto / total) * 100).toFixed(2)}%`;
  }

  puntos(valores: number[]): Array<[number, number]> {
    if (!valores.length) return [];
    const max = Math.max(...valores, 0);
    const min = Math.min(...valores, 0);
    const span = Math.max(max - min, 1);
    return valores.map((valor, indice) => {
      const x = valores.length === 1 ? 50 : (indice / (valores.length - 1)) * 100;
      const y = 92 - ((valor - min) / span) * 78;
      return [x, y];
    });
  }

  polilinea(valores: number[]): string {
    return this.puntos(valores).map((punto) => punto.join(',')).join(' ');
  }

  area(valores: number[]): string {
    const puntos = this.puntos(valores);
    if (!puntos.length) return '';
    return `${puntos[0][0]},100 ${puntos.map((punto) => punto.join(',')).join(' ')} ${puntos[puntos.length - 1][0]},100`;
  }

  mesCorto(periodo: string): string {
    const match = /^(\d{4})-(\d{2})/.exec(periodo);
    if (!match) return periodo.slice(0, 3);
    const fecha = new Date(Number(match[1]), Number(match[2]) - 1, 1);
    return new Intl.DateTimeFormat('es-PE', { month: 'short' }).format(fecha).replace('.', '');
  }

  ticks(valores: number[]): string[] {
    const max = Math.max(...valores, 0);
    const tope = max > 0 ? max : 0;
    return [1, 0.5, 0].map((factor) => new Intl.NumberFormat('es-PE', { maximumFractionDigits: 0 }).format(tope * factor));
  }

  cargar(sedeId = this.sedeFiltro.sedeId()): void {
    const ticket = ++this.carga;
    this.cargando = true;
    this.error = null;
    this.sedeId = sedeId;
    const sede = sedeId ? { sedeId } : {};
    const hoy = iso(new Date());
    const desde = new Date();
    desde.setDate(desde.getDate() - 30);
    const rango = { ...sede, desde: iso(desde), hasta: hoy, agrupacion: 'DIA' };
    forkJoin({
      hoy: this.reportes.GetApiV1ReportesVentasResumen({ ...sede, desde: hoy, hasta: hoy }).pipe(catchError(() => of(null))),
      resumen: this.reportes.GetApiV1ReportesVentasResumen(rango).pipe(catchError(() => of(null))),
      stock: this.stockApi.GetApiV1StockDashboard().pipe(catchError(() => of(null))),
      cxp: this.cxpApi.GetApiV1CuentasPorPagarDashboard().pipe(catchError(() => of(null))),
      ventas: this.ventasApi.GetApiV1Ventas({ ...sede, page: 0, size: 1, sort: 'createdAt,desc' }).pipe(catchError(() => of(null))),
      compras: this.comprasApi.GetApiV1Compras({ page: 0, size: 1, sort: 'createdAt,desc' }).pipe(catchError(() => of(null))),
      fracciones: this.fraccionesApi.GetApiV1CatalogoFracciones({ page: 0, size: 6, activo: true, sort: 'nombreFraccion,asc' }).pipe(catchError(() => of(null))),
      clientes: this.clientesApi.GetApiV1Clientes({ page: 0, size: 1 }).pipe(catchError(() => of(null))),
      cotizaciones: this.cotizacionesApi.GetApiV1Cotizaciones({ ...sede, page: 0, size: 1, sort: 'createdAt,desc' }).pipe(catchError(() => of(null))),
      kpis: this.esAdmin ? this.finanzas.GetApiV1FinanzasDashboardKpis(rango).pipe(catchError(() => of(null))) : of(null),
      evolucion: this.esAdmin ? this.finanzas.GetApiV1FinanzasDashboardEvolucionMensual({ ...sede, meses: 7 }).pipe(catchError(() => of(null))) : of(null),
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
      if (ticket !== this.carga) return;
      this.ventasHoy = data.hoy;
      this.resumen = data.resumen;
      this.stock = data.stock;
      this.cxp = data.cxp;
      this.kpis = data.kpis?.consolidado ?? null;
      const serie = (data.evolucion?.serie ?? []).filter((mes) => !!mes);
      if (serie.length) {
        this.ingresos = serie.map((mes) => Number(mes.ventasNetasReales ?? 0) || 0);
        this.gastos = serie.map((mes) => Number(mes.egresosOperativos ?? 0) || 0);
        this.etiquetas = serie.map((mes) => this.mesCorto(mes.periodo ?? ''));
      } else {
        const periodos = (data.resumen?.periodos ?? []).filter((periodo) => !!periodo);
        this.ingresos = periodos.map((periodo) => Number(periodo.neto ?? periodo.total ?? 0) || 0);
        this.gastos = [];
        this.etiquetas = periodos.map((periodo) => (periodo.etiqueta ?? '').slice(0, 3));
      }
      const filas = (data.fracciones?.content ?? []).filter((fila) => !!fila);
      this.productoFraccion = filas[0]?.productoNombre || this.productoFraccion;
      this.fracciones = filas.slice(0, 3).map((fila) => ({
        nombre: fila.productoNombre || 'Producto',
        etiqueta: fila.nombreFraccion || 'Fracción',
        ancho: Math.max(12, Math.min(100, Math.round(Number(fila.factorDescuentoKardex ?? 0.5) * 100))),
      }));
      this.clientes = data.clientes?.totalElements ?? 0;
      this.cotizaciones = data.cotizaciones?.totalElements ?? 0;
      const vacio = !data.hoy && !data.stock && !data.cxp && !data.ventas && !data.compras;
      this.error = vacio ? 'No hay conexión con el servidor. Reintenta cuando el backend esté disponible.' : null;
      this.cargando = false;
      },
      error: () => {
        if (ticket !== this.carga) return;
        this.cargando = false;
        this.error = 'No hay conexión con el servidor. Reintenta cuando el backend esté disponible.';
      },
    });
  }
}

export const DASHBOARD_ROUTES: Routes = [
  { path: '', component: DashboardPageComponent, title: 'Panel', data: { section: 'Inicio', breadcrumb: 'Panel' } },
];
