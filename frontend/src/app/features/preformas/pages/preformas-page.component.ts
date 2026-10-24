import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, effect, inject, untracked, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { AuthService } from '../../../services/auth.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
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
import { CajaSessionService } from '../../../shared/services/caja-session.service';
import { InventarioSyncService } from '../../../shared/services/inventario-sync.service';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { lineaProductoServicioOFraccion } from '../../../shared/utils/validators';
import { CatalogoFraccionesApiService } from '../../catalogo-fracciones/data-access/catalogo-fracciones.api';
import { PreformasApiService } from '../data-access/preformas.api';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-preformas-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, SelectComponent, TabsComponent, StatCardComponent, PenPipe],
  templateUrl: './preformas-page.component.html',
  styleUrls: ['./preformas-page.component.css'],
})
export class PreformasPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly caja = inject(CajaSessionService);
  private readonly inventario = inject(InventarioSyncService);
  readonly admin = inject(AuthService).isAdmin();
  tab = 'abiertas';
  readonly state = new PageState<Api.PreformaListadoDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'codigo', label: 'Código' },
    { key: 'clienteNombre', label: 'Cliente' },
    { key: 'vendedorNombre', label: 'Vendedor' },
    { key: 'estado', label: 'Estado', kind: 'badge', badge: { ABIERTA: 'info', CONFIRMADA: 'success', CANCELADA: 'danger', EXPIRADA: 'warning' } },
    { key: 'fechaExpiracion', label: 'Expira', kind: 'date' },
    { key: 'totalEstimado', label: 'Total', kind: 'money', align: 'end' },
  ];
  readonly filtros = this.fb.nonNullable.group({ sedeId: [''] });
  readonly form = this.fb.nonNullable.group({
    sedeId: ['', Validators.required],
    clienteId: [''],
    minutosVigencia: [60, [Validators.min(1)]],
    observaciones: ['', Validators.maxLength(500)],
    lineas: this.fb.array([this.linea()]),
  });
  readonly confirmarForm = this.fb.nonNullable.group({
    almacenId: ['', Validators.required],
    tipoComprobante: ['BOLETA' as Api.ConfirmarPreformaRequestDTO['tipoComprobante'], Validators.required],
    canalVenta: ['LOCAL' as NonNullable<Api.ConfirmarPreformaRequestDTO['canalVenta']>],
    metodoPago: ['EFECTIVO' as Api.PagoVentaCommandDTO['metodoPago'], Validators.required],
    monto: [0, [Validators.required, Validators.min(0.01)]],
    referencia: [''],
  });
  readonly cancelacion = this.fb.nonNullable.group({ motivo: ['', Validators.maxLength(255)] });
  sedes: SelectOption[] = [];
  clientes: SelectOption[] = [];
  productos: SelectOption[] = [];
  servicios: SelectOption[] = [];
  fracciones: SelectOption[] = [];
  almacenes: SelectOption[] = [];
  modal = false;
  editando = false;
  editId = '';
  guardando = false;
  errorFormulario: string | null = null;
  detalle: Api.PreformaVentaResponseDTO | null = null;
  detalleAbierto = false;
  confirmarAbierto = false;
  cancelarAbierto = false;
  confirmarEliminar = false;
  eliminando = false;
  seleccionado: Api.PreformaListadoDTO | null = null;
  dashboard: Api.PreformaDashboardDTO | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly api: PreformasApiService,
    private readonly fraccionesApi: CatalogoFraccionesApiService,
    private readonly lookup: LookupService,
    private readonly toast: ToastService,
  ) {
    effect(() => {
      this.lookup.revision();
      untracked(() => this.cargarCombos());
    });
  }

  ngOnInit(): void {
    this.cargar();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  get tabs(): TabItem[] {
    const base: TabItem[] = [{ id: 'abiertas', label: 'Abiertas' }, { id: 'vencidas', label: 'Vencidas' }];
    return this.admin ? [...base, { id: 'resumen', label: 'Resumen' }] : base;
  }

  get lineas() { return this.form.controls.lineas; }
  get turnoLocal(): string { return this.caja.id(); }

  cambiarTab(id: string): void {
    this.tab = id;
    this.state.page = 0;
    this.state.sort = id === 'vencidas' ? 'fechaExpiracion,desc' : 'createdAt,desc';
    this.cargar();
  }

  cargar(): void {
    if (this.tab === 'resumen') {
      this.dashboard = null;
      this.api.GetApiV1PreformasDashboard(this.state.query(this.filtros.getRawValue())).pipe(takeUntil(this.destroy$)).subscribe({
        next: (data) => { this.dashboard = data; this.pintar(); },
        error: (error: unknown) => this.toast.error(problemText(error)),
      });
      return;
    }
    if (this.tab === 'abiertas') {
      this.api.GetApiV1PreformasAbiertas().pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
        next: (rows) => {
          this.state.rows = rows ?? [];
          this.state.totalElements = this.state.rows.length;
          this.state.totalPages = this.state.rows.length ? 1 : 0;
          this.state.loading = false;
          this.state.error = null;
          this.state.status = null;
          this.pintar();
        },
        error: (error: unknown) => { this.state.fail(problemText(error), problemStatus(error)); this.pintar(); },
      });
      return;
    }
    this.api.GetApiV1PreformasVencidas(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => { this.state.apply(page); this.pintar(); },
      error: (error: unknown) => { this.state.fail(problemText(error), problemStatus(error)); this.pintar(); },
    });
  }

  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ sedeId: '' }); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }

  abrirCrear(): void {
    this.editando = false;
    this.editId = '';
    this.errorFormulario = null;
    this.form.reset({ sedeId: '', clienteId: '', minutosVigencia: 60, observaciones: '' });
    this.lineas.clear();
    this.lineas.push(this.linea());
    this.modal = true;
  }

  abrirEditar(row: unknown): void {
    const item = row as Api.PreformaListadoDTO;
    if (!item.id || item.estado !== 'ABIERTA') return;
    this.api.GetApiV1PreformasId({ id: item.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.editando = true;
        this.editId = data.id ?? '';
        this.errorFormulario = null;
        this.form.patchValue({ sedeId: data.sedeId ?? '', clienteId: data.clienteId ?? '', observaciones: data.observaciones ?? '' });
        this.lineas.clear();
        for (const linea of data.lineas ?? []) this.lineas.push(this.linea(linea.productoId, linea.servicioId, linea.fraccionId, linea.cantidad));
        if (!this.lineas.length) this.lineas.push(this.linea());
        this.modal = true;
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.PreformaVentaRequestDTO = {
      sedeId: value.sedeId,
      clienteId: value.clienteId || undefined,
      minutosVigencia: value.minutosVigencia ? Number(value.minutosVigencia) : undefined,
      observaciones: value.observaciones.trim() || undefined,
      lineas: value.lineas.map((linea) => ({
        cantidad: Number(linea.cantidad),
        productoId: linea.productoId || undefined,
        servicioId: linea.servicioId || undefined,
        fraccionId: linea.fraccionId || undefined,
      })),
    };
    this.guardando = true;
    const request = this.editando && this.editId
      ? this.api.PutApiV1PreformasId({ id: this.editId }, body)
      : this.api.PostApiV1Preformas({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.guardando = false;
        this.modal = false;
        this.toast.success(this.editando ? 'Preforma actualizada.' : 'Preforma abierta. El stock se descuenta al confirmar.');
        this.cargar();
      },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }

  ver(row: unknown): void {
    const item = row as Api.PreformaListadoDTO;
    if (!item.id) return;
    this.api.GetApiV1PreformasId({ id: item.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => { this.detalle = data; this.detalleAbierto = true; },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  abrirConfirmar(row: unknown): void {
    const item = row as Api.PreformaListadoDTO;
    this.seleccionado = item;
    this.confirmarForm.reset({
      almacenId: '',
      tipoComprobante: 'BOLETA',
      canalVenta: 'LOCAL',
      metodoPago: 'EFECTIVO',
      monto: item.totalEstimado ?? 0,
      referencia: '',
    });
    this.confirmarAbierto = true;
  }

  confirmarVenta(): void {
    if (this.guardando) return;
    this.confirmarForm.markAllAsTouched();
    if (!this.seleccionado?.id || this.confirmarForm.invalid) return;
    const value = this.confirmarForm.getRawValue();
    this.guardando = true;
    this.api.PostApiV1PreformasIdConfirmar({ id: this.seleccionado.id }, {
      almacenId: value.almacenId,
      tipoComprobante: value.tipoComprobante,
      canalVenta: value.canalVenta,
      pagos: [{ metodoPago: value.metodoPago, monto: Number(value.monto), referencia: value.referencia.trim() || undefined }],
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (resultado) => {
        this.guardando = false;
        this.confirmarAbierto = false;
        this.inventario.marcarCambio();
        this.caja.tocar();
        this.lookup.invalidate('productos');
        this.toast.success(`Venta ${resultado.venta?.serieCorrelativo ?? ''} registrada.`.trim());
        this.cargar();
      },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  abrirCancelar(row: unknown): void {
    this.seleccionado = row as Api.PreformaListadoDTO;
    this.cancelacion.reset({ motivo: '' });
    this.cancelarAbierto = true;
  }

  cancelar(): void {
    if (this.guardando || !this.seleccionado?.id || this.cancelacion.invalid) return;
    this.guardando = true;
    this.api.PostApiV1PreformasIdCancelar({ id: this.seleccionado.id }, {
      motivo: this.cancelacion.controls.motivo.value.trim() || undefined,
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.cancelarAbierto = false; this.toast.success('Preforma cancelada.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  pedirEliminar(row: unknown): void { this.seleccionado = row as Api.PreformaListadoDTO; this.confirmarEliminar = true; }

  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1PreformasId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmarEliminar = false; this.toast.success('Preforma eliminada.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmarEliminar = false; this.toast.error(problemText(error)); },
    });
  }

  esAbierta(row: unknown): boolean { return (row as Api.PreformaListadoDTO).estado === 'ABIERTA'; }
  agregarLinea(): void { this.lineas.push(this.linea()); }
  quitarLinea(index: number): void { if (this.lineas.length > 1) this.lineas.removeAt(index); }
  errorDe(nombre: 'sedeId' | 'minutosVigencia' | 'observaciones'): string | null { return fieldError(this.form.controls[nombre]); }
  errorLinea(index: number): string | null { return fieldError(this.lineas.at(index)); }

  private cargarCombos(): void {
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede'); });
    this.lookup.loadClientes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.clientes = this.lookup.options(rows, (row) => (row as Api.ClienteResponseDTO).nombre ?? 'Cliente'); });
    this.lookup.loadProductos().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.productos = this.lookup.options(rows, (row) => (row as Api.ProductoResponseDTO).nombre ?? 'Producto'); });
    this.lookup.loadServicios().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.servicios = this.lookup.options(rows, (row) => (row as Api.ServicioResponseDTO).nombre ?? 'Servicio'); });
    this.lookup.loadAlmacenes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.almacenes = this.lookup.options(rows, (row) => (row as Api.AlmacenResponseDTO).nombre ?? 'Almacén'); });
    this.fraccionesApi.GetApiV1CatalogoFracciones({ page: 0, size: 200, sort: 'nombreFraccion,asc' }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => {
        this.fracciones = (page.content ?? []).filter((row) => row.id).map((row) => ({
          value: row.id ?? '',
          label: row.productoNombre ? `${row.productoNombre} · ${row.nombreFraccion ?? 'Fracción'}` : (row.nombreFraccion ?? 'Fracción'),
        }));
      },
      error: () => { this.fracciones = []; },
    });
  }

  /** El fetch de Angular 22 no agenda detección de cambios; sin esto la tabla se queda en el esqueleto. */
  private pintar(): void {
    this.cdr.detectChanges();
  }

  private linea(productoId = '', servicioId = '', fraccionId = '', cantidad = 1) {
    return this.fb.nonNullable.group({
      productoId: [productoId ?? ''],
      servicioId: [servicioId ?? ''],
      fraccionId: [fraccionId ?? ''],
      cantidad: [cantidad ?? 1, [Validators.required, Validators.min(0.0001)]],
    }, { validators: lineaProductoServicioOFraccion });
  }
}

export const PREFORMAS_ROUTES: Routes = [
  { path: '', component: PreformasPageComponent, title: 'Preformas', data: { section: 'Ventas', breadcrumb: 'Preformas' } },
];
