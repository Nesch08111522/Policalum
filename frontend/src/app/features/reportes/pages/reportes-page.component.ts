import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { BarChartComponent, ChartPoint } from '../../../shared/components/bar-chart/bar-chart.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { TabsComponent } from '../../../shared/components/tabs/tabs.component';
import { ColumnDef, SelectOption, TabItem } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { LookupService } from '../../../shared/services/lookup.service';
import { problemStatus, problemText } from '../../../shared/utils/feedback';
import { PageState } from '../../../shared/utils/page-state';
import { ReportesApiService } from '../data-access/reportes.api';

function haceDias(dias: number): string {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() - dias);
  return fecha.toISOString().slice(0, 10);
}

@Component({
  selector: 'app-reportes-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, TabsComponent, FilterBarComponent, FormFieldComponent, SelectComponent, DataTableComponent, StatCardComponent, BarChartComponent, PenPipe],
  templateUrl: './reportes-page.component.html',
  styleUrls: ['./reportes-page.component.css'],
})
export class ReportesPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly tabs: TabItem[] = [
    { id: 'ventas', label: 'Ventas' },
    { id: 'rentabilidad', label: 'Rentabilidad' },
    { id: 'estrella', label: 'Productos estrella' },
    { id: 'clientes', label: 'Clientes' },
    { id: 'proveedores', label: 'Proveedores' },
    { id: 'arqueos', label: 'Arqueos' },
  ];
  tab = 'ventas';
  readonly ventas = new PageState<Api.VentaReporteDTO>();
  readonly filtros = this.fb.nonNullable.group({
    desde: [haceDias(30)],
    hasta: [new Date().toISOString().slice(0, 10)],
    sedeId: [''],
    canal: [''],
    estado: [''],
    agrupacion: ['DIA'],
  });
  readonly colVentas: ColumnDef[] = [
    { key: 'fecha', label: 'Fecha', kind: 'date' },
    { key: 'serieCorrelativo', label: 'Comprobante' },
    { key: 'cliente', label: 'Cliente' },
    { key: 'sede', label: 'Sede' },
    { key: 'estado', label: 'Estado', kind: 'badge', badge: { COMPLETADA: 'success', ANULADA: 'danger' } },
    { key: 'total', label: 'Total', kind: 'money', align: 'end' },
  ];
  readonly colRentabilidad: ColumnDef[] = [
    { key: 'productoNombre', label: 'Producto' },
    { key: 'cantidadNeta', label: 'Cantidad', kind: 'number', align: 'end' },
    { key: 'ingresoNetoReal', label: 'Ingreso', kind: 'money', align: 'end' },
    { key: 'costoEstimado', label: 'Costo', kind: 'money', align: 'end' },
  ];
  readonly colClientes: ColumnDef[] = [
    { key: 'posicion', label: '#' },
    { key: 'nombre', label: 'Cliente' },
    { key: 'compras', label: 'Compras', kind: 'number', align: 'end' },
    { key: 'montoNeto', label: 'Neto', kind: 'money', align: 'end' },
  ];
  readonly colProveedores: ColumnDef[] = [
    { key: 'posicion', label: '#' },
    { key: 'nombre', label: 'Proveedor' },
    { key: 'compras', label: 'Compras', kind: 'number', align: 'end' },
    { key: 'montoRecibido', label: 'Recibido', kind: 'money', align: 'end' },
  ];
  readonly colArqueos: ColumnDef[] = [
    { key: 'sede', label: 'Sede' },
    { key: 'cajero', label: 'Cajero' },
    { key: 'estado', label: 'Estado', kind: 'badge' },
    { key: 'resultado', label: 'Resultado', kind: 'badge', badge: { CUADRADO: 'success', FALTANTE: 'danger', SOBRANTE: 'warning', ABIERTO: 'info', SIN_DECLARAR: 'neutral' } },
    { key: 'diferencia', label: 'Diferencia', kind: 'money', align: 'end' },
  ];
  sedes: SelectOption[] = [];
  resumen: Api.VentasResumenDTO | null = null;
  puntos: ChartPoint[] = [];
  rentabilidad: Api.RentabilidadItemDTO[] = [];
  estrella: Api.RentabilidadItemDTO[] = [];
  clientes: Api.ClienteFrecuenteDTO[] = [];
  proveedores: Api.ProveedorRankingDTO[] = [];
  arqueos: Api.ArqueoTurnoDTO[] = [];
  resumenArqueo: Api.ResumenArqueosDTO | null = null;
  loading = false;
  error: string | null = null;
  status: number | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: ReportesApiService, private readonly lookup: LookupService) {
    this.ventas.sort = 'fecha,desc';
  }

  ngOnInit(): void {
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede')));
    this.cargar();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  cambiarTab(id: string): void { this.tab = id; this.cargar(); }
  filtrar(): void { this.ventas.page = 0; this.cargar(); }
  limpiar(): void {
    this.filtros.reset({ desde: haceDias(30), hasta: new Date().toISOString().slice(0, 10), sedeId: '', canal: '', estado: '', agrupacion: 'DIA' });
    this.filtrar();
  }

  cargar(): void {
    this.loading = true;
    this.error = null;
    const params = this.filtros.getRawValue();
    if (this.tab === 'ventas') {
      this.api.GetApiV1ReportesVentasResumen(params).pipe(takeUntil(this.destroy$)).subscribe({
        next: (data) => { this.resumen = data; this.puntos = (data.periodos ?? []).map((periodo) => ({ label: periodo.etiqueta ?? '', value: Number(periodo.total ?? 0) })); },
        error: () => { this.resumen = null; this.puntos = []; },
      });
      this.ventas.loading = true;
      this.api.GetApiV1ReportesVentas(this.ventas.query(params)).pipe(takeUntil(this.destroy$)).subscribe({
        next: (page) => { this.ventas.apply(page); this.loading = false; },
        error: (error: unknown) => { this.ventas.fail(problemText(error), problemStatus(error)); this.loading = false; this.error = problemText(error); this.status = problemStatus(error); },
      });
      return;
    }
    const done = (): void => { this.loading = false; };
    const fail = (error: unknown): void => { this.loading = false; this.error = problemText(error); this.status = problemStatus(error); };
    if (this.tab === 'rentabilidad') {
      this.api.GetApiV1ReportesProductosRentabilidad(params).pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => { this.rentabilidad = data.items ?? []; done(); }, error: fail });
    } else if (this.tab === 'estrella') {
      this.api.GetApiV1ReportesProductosEstrella(params).pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => { this.estrella = data.items ?? []; done(); }, error: fail });
    } else if (this.tab === 'clientes') {
      this.api.GetApiV1ReportesClientesFrecuentes(params).pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => { this.clientes = data.items ?? []; done(); }, error: fail });
    } else if (this.tab === 'proveedores') {
      this.api.GetApiV1ReportesProveedoresRanking(params).pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => { this.proveedores = data.items ?? []; done(); }, error: fail });
    } else {
      this.api.GetApiV1ReportesCajaArqueos(params).pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => { this.arqueos = data.turnos ?? []; this.resumenArqueo = data.resumen ?? null; done(); }, error: fail });
    }
  }

  cambiarPagina(page: number): void { this.ventas.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.ventas.size = size; this.ventas.page = 0; this.cargar(); }
}

export const REPORTES_ROUTES: Routes = [
  { path: '', component: ReportesPageComponent, title: 'Reportes', data: { section: 'Análisis', breadcrumb: 'Reportes' } },
];
