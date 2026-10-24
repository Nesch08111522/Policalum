import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { AuthService } from '../../../services/auth.service';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { TabsComponent } from '../../../shared/components/tabs/tabs.component';
import { ColumnDef, SelectOption, TabItem } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { InventarioSyncService } from '../../../shared/services/inventario-sync.service';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { VentasApiService } from '../../ventas/data-access/ventas.api';
import { DevolucionesApiService } from '../data-access/devoluciones.api';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-devoluciones-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, FormFieldComponent, SelectComponent, TabsComponent, StatCardComponent, PenPipe],
  templateUrl: './devoluciones-page.component.html',
  styleUrls: ['./devoluciones-page.component.css'],
})
export class DevolucionesPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly inventario = inject(InventarioSyncService);
  readonly admin = inject(AuthService).isAdmin();
  tab = 'lista';
  readonly state = new PageState<Api.DevolucionListadoDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'codigo', label: 'Código' },
    { key: 'ventaSerie', label: 'Venta' },
    { key: 'clienteNombre', label: 'Cliente' },
    { key: 'sedeNombre', label: 'Sede' },
    { key: 'estado', label: 'Estado', kind: 'badge', badge: { PENDIENTE: 'warning', APROBADA: 'success', RECHAZADA: 'danger' } },
    { key: 'fechaSolicitud', label: 'Solicitud', kind: 'date', sortable: true },
    { key: 'montoTotal', label: 'Monto', kind: 'money', align: 'end' },
  ];
  readonly filtros = this.fb.nonNullable.group({ sedeId: [''], estado: [''], desde: [''], hasta: [''] });
  readonly form = this.fb.nonNullable.group({
    ventaId: ['', Validators.required],
    motivo: ['', [Validators.required, Validators.maxLength(500)]],
    lineas: this.fb.array<ReturnType<DevolucionesPageComponent['linea']>>([]),
  });
  readonly rechazo = this.fb.nonNullable.group({ motivo: ['', [Validators.required, Validators.maxLength(255)]] });
  readonly aprobacion = this.fb.nonNullable.group({ observacion: ['', Validators.maxLength(255)] });
  sedes: SelectOption[] = [];
  ventas: SelectOption[] = [];
  ventaDetalle: Api.VentaDetalleResponseDTO | null = null;
  modal = false;
  guardando = false;
  errorFormulario: string | null = null;
  detalle: Api.DevolucionResponseDTO | null = null;
  detalleAbierto = false;
  aprobarAbierto = false;
  rechazarAbierto = false;
  seleccionado: Api.DevolucionListadoDTO | null = null;
  dashboard: Api.DevolucionDashboardDTO | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly api: DevolucionesApiService,
    private readonly ventasApi: VentasApiService,
    private readonly lookup: LookupService,
    private readonly toast: ToastService,
  ) {
    this.state.sort = 'fechaSolicitud,desc';
  }

  ngOnInit(): void {
    this.cargar();
    this.form.controls.ventaId.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.elegirVenta());
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => {
      this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede');
    });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  get tabs(): TabItem[] {
    const base: TabItem[] = [{ id: 'lista', label: 'Devoluciones' }];
    return this.admin ? [...base, { id: 'resumen', label: 'Resumen' }] : base;
  }

  get lineas() { return this.form.controls.lineas; }

  cambiarTab(id: string): void { this.tab = id; this.cargar(); }

  cargar(): void {
    if (this.tab === 'resumen') {
      this.dashboard = null;
      this.api.GetApiV1DevolucionesDashboard(this.state.query(this.filtros.getRawValue())).pipe(takeUntil(this.destroy$)).subscribe({
        next: (data) => { this.dashboard = data; this.pintar(); },
        error: (error: unknown) => this.toast.error(problemText(error)),
      });
      return;
    }
    this.api.GetApiV1Devoluciones(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => { this.state.apply(page); this.pintar(); },
      error: (error: unknown) => { this.state.fail(problemText(error), problemStatus(error)); this.pintar(); },
    });
  }

  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ sedeId: '', estado: '', desde: '', hasta: '' }); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }

  abrirCrear(): void {
    this.errorFormulario = null;
    this.ventaDetalle = null;
    this.form.reset({ ventaId: '', motivo: '' });
    this.lineas.clear();
    this.modal = true;
    this.ventasApi.GetApiV1Ventas({ estado: 'COMPLETADA', page: 0, size: 50, sort: 'createdAt,desc' }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => {
        this.ventas = (page.content ?? []).filter((row) => row.id).map((row) => ({
          value: row.id ?? '',
          label: `${row.serieCorrelativo ?? 'Venta'} · ${row.clienteNombre || 'Sin cliente'} · S/ ${row.total ?? 0}`,
        }));
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  elegirVenta(): void {
    const ventaId = this.form.controls.ventaId.value;
    this.lineas.clear();
    this.ventaDetalle = null;
    if (!ventaId) return;
    this.ventasApi.GetApiV1VentasId({ id: ventaId }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (venta) => {
        this.ventaDetalle = venta;
        for (const linea of venta.lineas ?? []) {
          if (!linea.id) continue;
          this.lineas.push(this.linea(linea));
        }
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    const lineas = value.lineas.filter((linea) => Number(linea.cantidad) > 0);
    if (this.form.controls.ventaId.invalid || this.form.controls.motivo.invalid || !lineas.length) {
      this.errorFormulario = lineas.length ? null : 'Indica la cantidad a devolver en al menos una línea.';
      return;
    }
    const body: Api.DevolucionRequestDTO = {
      ventaId: value.ventaId,
      motivo: value.motivo.trim(),
      lineas: lineas.map((linea) => ({
        detalleVentaId: linea.detalleVentaId,
        cantidad: Number(linea.cantidad),
        estadoProducto: linea.estadoProducto,
        observacion: linea.observacion.trim() || undefined,
      })),
    };
    this.guardando = true;
    this.errorFormulario = null;
    this.api.PostApiV1Devoluciones({}, body).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.toast.success('Devolución registrada. Queda pendiente de aprobación.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }

  ver(row: unknown): void {
    const item = row as Api.DevolucionListadoDTO;
    if (!item.id) return;
    this.api.GetApiV1DevolucionesId({ id: item.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => { this.detalle = data; this.detalleAbierto = true; },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  abrirAprobar(row: unknown): void { this.seleccionado = row as Api.DevolucionListadoDTO; this.aprobacion.reset({ observacion: '' }); this.aprobarAbierto = true; }
  abrirRechazar(row: unknown): void { this.seleccionado = row as Api.DevolucionListadoDTO; this.rechazo.reset({ motivo: '' }); this.rechazarAbierto = true; }
  pendiente(row: unknown): boolean { return (row as Api.DevolucionListadoDTO).estado === 'PENDIENTE'; }

  aprobar(): void {
    if (this.guardando || !this.seleccionado?.id) return;
    this.guardando = true;
    const observacion = this.aprobacion.controls.observacion.value.trim();
    this.api.PostApiV1DevolucionesIdAprobar({ id: this.seleccionado.id }, { observacion: observacion || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.guardando = false;
        this.aprobarAbierto = false;
        this.inventario.marcarCambio();
        this.toast.success('Devolución aprobada. El kardex ya registró el reingreso o la merma.');
        this.cargar();
      },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  rechazar(): void {
    this.rechazo.markAllAsTouched();
    if (this.guardando || !this.seleccionado?.id || this.rechazo.invalid) return;
    this.guardando = true;
    this.api.PostApiV1DevolucionesIdRechazar({ id: this.seleccionado.id }, { motivo: this.rechazo.controls.motivo.value.trim() }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.rechazarAbierto = false; this.toast.success('Devolución rechazada.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  errorDe(nombre: 'ventaId' | 'motivo'): string | null { return fieldError(this.form.controls[nombre]); }

  /** El fetch de Angular 22 no agenda detección de cambios; sin esto la tabla se queda en el esqueleto. */
  private pintar(): void { this.cdr.detectChanges(); }

  private linea(origen: Api.LineaVentaResponseDTO) {
    const vendida = Number(origen.cantidad ?? 0);
    return this.fb.nonNullable.group({
      detalleVentaId: [origen.id ?? '', Validators.required],
      nombre: [origen.productoNombre || origen.servicioNombre || origen.fraccionNombre || 'Ítem'],
      vendida: [vendida],
      cantidad: [vendida, [Validators.required, Validators.min(0), Validators.max(vendida || 0.0001)]],
      estadoProducto: ['BUENO' as Api.DetalleDevolucionRequestDTO['estadoProducto'], Validators.required],
      observacion: [''],
    });
  }
}

export const DEVOLUCIONES_ROUTES: Routes = [
  { path: '', component: DevolucionesPageComponent, title: 'Devoluciones', data: { section: 'Ventas', breadcrumb: 'Devoluciones' } },
];
