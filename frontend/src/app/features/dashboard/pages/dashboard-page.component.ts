import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { RouterLink, Routes } from '@angular/router';
import { Subject, forkJoin, of, takeUntil } from 'rxjs';
import { catchError } from 'rxjs/operators';
import * as Api from '../../../core/api/openapi.types';
import { TokenService } from '../../../services/token.service';
import { BarChartComponent, ChartPoint } from '../../../shared/components/bar-chart/bar-chart.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { QUICK_LINKS } from '../../../layout/navigation';
import { ComprasApiService } from '../../compras/data-access/compras.api';
import { CuentasPorPagarApiService } from '../../cuentas-por-pagar/data-access/cuentas-por-pagar.api';
import { FinanzasApiService } from '../../finanzas/data-access/finanzas.api';
import { ReportesApiService } from '../../reportes/data-access/reportes.api';
import { StockApiService } from '../../stock/data-access/stock.api';
import { VentasApiService } from '../../ventas/data-access/ventas.api';

function iso(fecha: Date): string { return fecha.toISOString().slice(0, 10); }

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent, StatCardComponent, BarChartComponent, PenPipe],
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.css'],
})
export class DashboardPageComponent implements OnInit, OnDestroy {
  private readonly cd = inject(ChangeDetectorRef);
  readonly accesos = QUICK_LINKS;
  readonly esAdmin: boolean;
  cargando = true;
  error: string | null = null;
  ventasHoy: Api.VentasResumenDTO | null = null;
  stock: Api.StockDashboardDTO | null = null;
  cxp: Api.ResumenCxPDashboardDTO | null = null;
  kpis: Api.KpisFinancierosDTO | null = null;
  patrimonio: Api.PatrimonioResponseDTO | null = null;
  puntos: ChartPoint[] = [];
  ventas: Api.VentaListadoResponseDTO[] = [];
  compras: Api.CompraListadoDTO[] = [];
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly token: TokenService,
    private readonly reportes: ReportesApiService,
    private readonly stockApi: StockApiService,
    private readonly cxpApi: CuentasPorPagarApiService,
    private readonly finanzas: FinanzasApiService,
    private readonly ventasApi: VentasApiService,
    private readonly comprasApi: ComprasApiService,
  ) {
    this.esAdmin = this.token.getRol() === 'ROLE_ADMIN';
  }

  ngOnInit(): void { this.cargar(); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  cargar(): void {
    this.cargando = true;
    this.error = null;
    const hoy = iso(new Date());
    const desde = new Date();
    desde.setDate(desde.getDate() - 30);
    const rango = { desde: iso(desde), hasta: hoy, agrupacion: 'DIA' };
    forkJoin({
      hoy: this.reportes.GetApiV1ReportesVentasResumen({ desde: hoy, hasta: hoy }).pipe(catchError(() => of(null))),
      resumen: this.reportes.GetApiV1ReportesVentasResumen(rango).pipe(catchError(() => of(null))),
      stock: this.stockApi.GetApiV1StockDashboard().pipe(catchError(() => of(null))),
      cxp: this.cxpApi.GetApiV1CuentasPorPagarDashboard().pipe(catchError(() => of(null))),
      ventas: this.ventasApi.GetApiV1Ventas({ page: 0, size: 5, sort: 'createdAt,desc' }).pipe(catchError(() => of(null))),
      compras: this.comprasApi.GetApiV1Compras({ page: 0, size: 5, sort: 'createdAt,desc' }).pipe(catchError(() => of(null))),
      kpis: this.esAdmin ? this.finanzas.GetApiV1FinanzasDashboardKpis(rango).pipe(catchError(() => of(null))) : of(null),
      patrimonio: this.esAdmin ? this.finanzas.GetApiV1FinanzasDashboardPatrimonio().pipe(catchError(() => of(null))) : of(null),
    }).pipe(takeUntil(this.destroy$)).subscribe((data) => {
      this.ventasHoy = data.hoy;
      this.stock = data.stock;
      this.cxp = data.cxp;
      this.kpis = data.kpis?.consolidado ?? null;
      this.patrimonio = data.patrimonio;
      this.puntos = (data.resumen?.periodos ?? []).map((periodo) => ({ label: (periodo.etiqueta ?? '').slice(0, 6), value: Number(periodo.total ?? 0) }));
      this.ventas = data.ventas?.content ?? [];
      this.compras = data.compras?.content ?? [];
      const vacio = !data.hoy && !data.stock && !data.cxp && !data.ventas && !data.compras;
      this.error = vacio ? 'No hay conexión con el servidor. Reintenta cuando el backend esté disponible.' : null;
      this.cargando = false;
      this.cd.markForCheck();
    });
  }
}

export const DASHBOARD_ROUTES: Routes = [
  { path: '', component: DashboardPageComponent, title: 'Panel', data: { section: 'Inicio', breadcrumb: 'Panel' } },
];
