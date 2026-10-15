import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { TabsComponent } from '../../../shared/components/tabs/tabs.component';
import { ColumnDef, SelectOption, TabItem } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { PageState } from '../../../shared/utils/page-state';
import { lineaProductoOServicio } from '../../../shared/utils/validators';
import { CotizacionesApiService } from '../data-access/cotizaciones.api';

const TURNO_KEY = 'erp-turno-caja';

@Component({
  selector: 'app-cotizaciones-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, SelectComponent, TabsComponent, PenPipe],
  templateUrl: './cotizaciones-page.component.html',
  styleUrls: ['./cotizaciones-page.component.css'],
})
export class CotizacionesPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly tabs: TabItem[] = [{ id: 'lista', label: 'Cotizaciones' }, { id: 'vencidas', label: 'Vencidas' }];
  tab = 'lista';
  readonly state = new PageState<Api.CotizacionListadoDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'clienteNombre', label: 'Cliente' },
    { key: 'estado', label: 'Estado', kind: 'badge', badge: { EMITIDA: 'info', ACEPTADA: 'success', RECHAZADA: 'danger', VENCIDA: 'warning', CONVERTIDA: 'neutral' } },
    { key: 'fechaVigencia', label: 'Vigencia', kind: 'date' },
    { key: 'createdAt', label: 'Creada', kind: 'date', sortable: true },
    { key: 'total', label: 'Total', kind: 'money', align: 'end' },
  ];
  readonly filtros = this.fb.nonNullable.group({ clienteId: [''], sedeId: [''], estado: [''], desde: [''], hasta: [''] });
  readonly form = this.fb.nonNullable.group({
    sedeId: ['', Validators.required],
    clienteId: [''],
    fechaVigencia: [''],
    observaciones: ['', Validators.maxLength(500)],
    lineas: this.fb.array([this.linea()]),
  });
  readonly convertir = this.fb.nonNullable.group({
    almacenId: ['', Validators.required],
    turnoCajaId: [this.turno(), Validators.required],
    tipoComprobante: ['BOLETA' as Api.ConvertirCotizacionRequestDTO['tipoComprobante'], Validators.required],
    canalVenta: ['LOCAL' as NonNullable<Api.ConvertirCotizacionRequestDTO['canalVenta']>],
    metodoPago: ['EFECTIVO' as Api.PagoVentaCommandDTO['metodoPago'], Validators.required],
    monto: [0, [Validators.required, Validators.min(0.01)]],
  });
  readonly rechazo = this.fb.nonNullable.group({ motivo: ['', [Validators.required, Validators.maxLength(255)]] });
  sedes: SelectOption[] = [];
  clientes: SelectOption[] = [];
  productos: SelectOption[] = [];
  servicios: SelectOption[] = [];
  almacenes: SelectOption[] = [];
  modal = false;
  editando = false;
  editId = '';
  guardando = false;
  errorFormulario: string | null = null;
  detalle: Api.CotizacionResponseDTO | null = null;
  detalleAbierto = false;
  convertirAbierto = false;
  rechazarAbierto = false;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.CotizacionListadoDTO | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: CotizacionesApiService, private readonly lookup: LookupService, private readonly toast: ToastService) {}

  ngOnInit(): void {
    this.cargar();
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede')));
    this.lookup.loadClientes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.clientes = this.lookup.options(rows, (row) => (row as Api.ClienteResponseDTO).nombre ?? 'Cliente')));
    this.lookup.loadProductos().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.productos = this.lookup.options(rows, (row) => (row as Api.ProductoResponseDTO).nombre ?? 'Producto')));
    this.lookup.loadServicios().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.servicios = this.lookup.options(rows, (row) => (row as Api.ServicioResponseDTO).nombre ?? 'Servicio')));
    this.lookup.loadAlmacenes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.almacenes = this.lookup.options(rows, (row) => (row as Api.AlmacenResponseDTO).nombre ?? 'Almacén')));
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  get lineas() { return this.form.controls.lineas; }

  cambiarTab(id: string): void { this.tab = id; this.state.page = 0; this.cargar(); }

  cargar(): void {
    this.state.loading = true;
    if (this.tab === 'vencidas') {
      this.api.GetApiV1CotizacionesVencidas().pipe(takeUntil(this.destroy$)).subscribe({
        next: (rows) => {
          this.state.rows = rows ?? [];
          this.state.totalElements = this.state.rows.length;
          this.state.totalPages = this.state.rows.length ? 1 : 0;
          this.state.loading = false;
          this.state.error = null;
          this.state.status = null;
        },
        error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
      });
      return;
    }
    this.api.GetApiV1Cotizaciones(this.state.query(this.filtros.getRawValue())).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }

  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ clienteId: '', sedeId: '', estado: '', desde: '', hasta: '' }); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }

  abrirCrear(): void {
    this.editando = false;
    this.editId = '';
    this.errorFormulario = null;
    this.form.reset({ sedeId: '', clienteId: '', fechaVigencia: '', observaciones: '' });
    this.lineas.clear();
    this.lineas.push(this.linea());
    this.modal = true;
  }

  abrirEditar(row: unknown): void {
    const item = row as Api.CotizacionListadoDTO;
    if (!item.id || item.estado !== 'EMITIDA') return;
    this.api.GetApiV1CotizacionesId({ id: item.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.editando = true;
        this.editId = data.id ?? '';
        this.form.patchValue({ sedeId: data.sedeId ?? '', clienteId: data.clienteId ?? '', fechaVigencia: (data.fechaVigencia ?? '').slice(0, 10), observaciones: data.observaciones ?? '' });
        this.lineas.clear();
        for (const linea of data.lineas ?? []) this.lineas.push(this.linea(linea.productoId, linea.servicioId, linea.cantidad));
        if (!this.lineas.length) this.lineas.push(this.linea());
        this.modal = true;
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.CotizacionRequestDTO = {
      sedeId: value.sedeId,
      clienteId: value.clienteId || undefined,
      fechaVigencia: value.fechaVigencia || undefined,
      observaciones: value.observaciones.trim() || undefined,
      lineas: value.lineas.map((linea) => ({ cantidad: Number(linea.cantidad), productoId: linea.productoId || undefined, servicioId: linea.servicioId || undefined })),
    };
    this.guardando = true;
    const request = this.editando && this.editId ? this.api.PutApiV1CotizacionesId({ id: this.editId }, body) : this.api.PostApiV1Cotizaciones({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.toast.success(this.editando ? 'Cotización actualizada.' : 'Cotización emitida.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }

  ver(row: unknown): void {
    const item = row as Api.CotizacionListadoDTO;
    if (!item.id) return;
    this.api.GetApiV1CotizacionesId({ id: item.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => { this.detalle = data; this.detalleAbierto = true; },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  aceptar(row: unknown): void {
    const item = row as Api.CotizacionListadoDTO;
    if (!item.id) return;
    this.api.PostApiV1CotizacionesIdAceptar({ id: item.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.toast.success('Cotización aceptada.'); this.cargar(); },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  abrirRechazar(row: unknown): void { this.seleccionado = row as Api.CotizacionListadoDTO; this.rechazo.reset({ motivo: '' }); this.rechazarAbierto = true; }
  rechazar(): void {
    this.rechazo.markAllAsTouched();
    if (!this.seleccionado?.id || this.rechazo.invalid) return;
    this.guardando = true;
    this.api.PostApiV1CotizacionesIdRechazar({ id: this.seleccionado.id }, { motivo: this.rechazo.controls.motivo.value.trim() }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.rechazarAbierto = false; this.toast.success('Cotización rechazada.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  abrirConvertir(row: unknown): void {
    const item = row as Api.CotizacionListadoDTO;
    this.seleccionado = item;
    this.convertir.reset({ almacenId: '', turnoCajaId: this.turno(), tipoComprobante: 'BOLETA', canalVenta: 'LOCAL', metodoPago: 'EFECTIVO', monto: item.total ?? 0 });
    this.convertirAbierto = true;
  }

  convertirVenta(): void {
    this.convertir.markAllAsTouched();
    if (!this.seleccionado?.id || this.convertir.invalid) return;
    const value = this.convertir.getRawValue();
    this.guardando = true;
    this.api.PostApiV1CotizacionesIdConvertir({ id: this.seleccionado.id }, {
      almacenId: value.almacenId,
      turnoCajaId: value.turnoCajaId.trim(),
      tipoComprobante: value.tipoComprobante,
      canalVenta: value.canalVenta,
      pagos: [{ metodoPago: value.metodoPago, monto: Number(value.monto) }],
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (venta) => { this.guardando = false; this.convertirAbierto = false; this.toast.success(`Convertida en venta ${venta.serieCorrelativo ?? ''}.`.trim()); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  pedirEliminar(row: unknown): void { this.seleccionado = row as Api.CotizacionListadoDTO; this.confirmar = true; }
  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1CotizacionesId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.toast.success('Cotización eliminada.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }

  esEmitida(row: unknown): boolean { return (row as Api.CotizacionListadoDTO).estado === 'EMITIDA'; }
  puedeConvertir(row: unknown): boolean { const estado = (row as Api.CotizacionListadoDTO).estado; return estado === 'EMITIDA' || estado === 'ACEPTADA'; }
  agregarLinea(): void { this.lineas.push(this.linea()); }
  quitarLinea(index: number): void { if (this.lineas.length > 1) this.lineas.removeAt(index); }
  errorDe(nombre: 'sedeId' | 'observaciones'): string | null { return fieldError(this.form.controls[nombre]); }

  private linea(productoId = '', servicioId = '', cantidad = 1) {
    return this.fb.nonNullable.group({ productoId: [productoId ?? ''], servicioId: [servicioId ?? ''], cantidad: [cantidad ?? 1, [Validators.required, Validators.min(0.0001)]] }, { validators: lineaProductoOServicio });
  }

  private turno(): string { return typeof sessionStorage === 'undefined' ? '' : sessionStorage.getItem(TURNO_KEY) ?? ''; }
}

export const COTIZACIONES_ROUTES: Routes = [
  { path: '', component: CotizacionesPageComponent, title: 'Cotizaciones', data: { section: 'Ventas', breadcrumb: 'Cotizaciones' } },
];
