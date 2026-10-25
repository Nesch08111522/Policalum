import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { ColumnDef, SelectOption } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { InventarioSyncService } from '../../../shared/services/inventario-sync.service';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { ComprasApiService } from '../data-access/compras.api';

function hoy(): string {
  const fecha = new Date();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-compras-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, SelectComponent, StatCardComponent, PenPipe],
  templateUrl: './compras-page.component.html',
  styleUrls: ['./compras-page.component.css'],
})
export class ComprasPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.CompraListadoDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'numeroOrdenCompra', label: 'Orden', sortable: true },
    { key: 'proveedorNombre', label: 'Proveedor' },
    { key: 'fechaEmision', label: 'Emisión', kind: 'date', sortable: true },
    { key: 'estadoCompra', label: 'Compra', kind: 'badge', badge: { BORRADOR: 'warning', RECIBIDA: 'success', ANULADA: 'danger' } },
    { key: 'estadoPago', label: 'Pago', kind: 'badge', badge: { PENDIENTE: 'warning', PAGADO_PARCIAL: 'info', PAGADO: 'success' } },
    { key: 'total', label: 'Total', kind: 'money', align: 'end' },
  ];
  readonly filtros = this.fb.nonNullable.group({ proveedorId: [''], sedeId: [''], estado: [''], desde: [''], hasta: [''] });
  readonly form = this.fb.nonNullable.group({
    proveedorId: ['', Validators.required],
    sedeId: ['', Validators.required],
    fechaEmision: [hoy(), Validators.required],
    moneda: ['PEN' as NonNullable<Api.CompraRequestDTO['moneda']>],
    numeroFacturaProveedor: ['', Validators.maxLength(40)],
    observaciones: ['', Validators.maxLength(500)],
    detalles: this.fb.array([this.linea()]),
  });
  readonly recepcion = this.fb.nonNullable.group({ numeroFacturaProveedor: ['', Validators.maxLength(40)], observaciones: ['', Validators.maxLength(500)] });
  readonly anulacion = this.fb.nonNullable.group({ motivo: ['', [Validators.required, Validators.maxLength(255)]] });
  proveedores: SelectOption[] = [];
  sedes: SelectOption[] = [];
  productos: SelectOption[] = [];
  almacenes: SelectOption[] = [];
  dashboard: Record<string, unknown> | null = null;
  modal = false;
  editando = false;
  editId = '';
  guardando = false;
  errorFormulario: string | null = null;
  detalle: Api.CompraResponseDTO | null = null;
  detalleAbierto = false;
  recibirAbierto = false;
  anularAbierto = false;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.CompraListadoDTO | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly api: ComprasApiService,
    private readonly lookup: LookupService,
    private readonly inventario: InventarioSyncService,
    private readonly toast: ToastService,
  ) {
    this.state.sort = 'fechaEmision,desc';
  }

  ngOnInit(): void {
    this.cargar();
    this.refrescarDashboard();
    this.lookup.loadProveedores().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.proveedores = this.lookup.options(rows, (row) => (row as Api.ProveedorResponseDTO).nombreRazon ?? 'Proveedor')));
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede')));
    this.lookup.loadProductos().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.productos = this.lookup.options(rows, (row) => `${(row as Api.ProductoResponseDTO).codigoSku ?? ''} ${(row as Api.ProductoResponseDTO).nombre ?? ''}`.trim())));
    this.lookup.loadAlmacenes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.almacenes = this.lookup.options(rows, (row) => (row as Api.AlmacenResponseDTO).nombre ?? 'Almacén')));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get detalles() { return this.form.controls.detalles; }

  cargar(): void {
    this.api.GetApiV1Compras(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }

  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ proveedorId: '', sedeId: '', estado: '', desde: '', hasta: '' }); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }

  abrirCrear(): void {
    this.editando = false;
    this.editId = '';
    this.errorFormulario = null;
    this.form.reset({ proveedorId: '', sedeId: '', fechaEmision: hoy(), moneda: 'PEN', numeroFacturaProveedor: '', observaciones: '' });
    this.vaciarLineas();
    this.modal = true;
  }

  abrirEditar(row: unknown): void {
    const compra = row as Api.CompraListadoDTO;
    if (!compra.id || compra.estadoCompra !== 'BORRADOR') return;
    this.api.GetApiV1ComprasId({ id: compra.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.editando = true;
        this.editId = data.id ?? '';
        this.errorFormulario = null;
        this.form.patchValue({
          proveedorId: data.proveedorId ?? '',
          sedeId: data.sedeId ?? '',
          fechaEmision: (data.fechaEmision ?? hoy()).slice(0, 10),
          moneda: data.moneda ?? 'PEN',
          numeroFacturaProveedor: data.numeroFacturaProveedor ?? '',
          observaciones: data.observaciones ?? '',
        });
        this.detalles.clear();
        for (const linea of data.detalles ?? []) {
          this.detalles.push(this.linea(linea.productoId, linea.almacenId, linea.cantidad, linea.precioUnitarioConIgv));
        }
        if (!this.detalles.length) this.detalles.push(this.linea());
        this.modal = true;
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  ver(row: unknown): void {
    const compra = row as Api.CompraListadoDTO;
    if (!compra.id) return;
    this.api.GetApiV1ComprasId({ id: compra.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => { this.detalle = data; this.detalleAbierto = true; },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.CompraRequestDTO = {
      proveedorId: value.proveedorId,
      sedeId: value.sedeId,
      fechaEmision: value.fechaEmision,
      moneda: value.moneda,
      numeroFacturaProveedor: value.numeroFacturaProveedor.trim() || undefined,
      observaciones: value.observaciones.trim() || undefined,
      detalles: value.detalles.map((linea) => ({
        productoId: linea.productoId,
        almacenId: linea.almacenId,
        cantidad: Number(linea.cantidad),
        precioUnitarioConIgv: Number(linea.precioUnitarioConIgv),
      })),
    };
    this.guardando = true;
    this.errorFormulario = null;
    const request = this.editando && this.editId ? this.api.PutApiV1ComprasId({ id: this.editId }, body) : this.api.PostApiV1Compras({}, body);
    const editando = this.editando;
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: (compra) => {
        this.guardando = false;
        this.modal = false;
        this.form.reset({ proveedorId: '', sedeId: '', fechaEmision: hoy(), moneda: 'PEN', numeroFacturaProveedor: '', observaciones: '' });
        this.vaciarLineas();
        if (editando) this.reemplazarFila(compra);
        else this.insertarFila(compra);
        if (!editando) this.inventario.marcarCambio();
        this.refrescarDashboard();
        this.toast.success(editando
          ? 'Compra actualizada.'
          : `Compra ${compra.numeroOrdenCompra ?? ''} registrada. El stock, el kardex y la cuenta por pagar quedaron grabados.`);
      },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }

  abrirRecibir(row: unknown): void {
    this.seleccionado = row as Api.CompraListadoDTO;
    this.recepcion.reset({ numeroFacturaProveedor: '', observaciones: '' });
    this.recibirAbierto = true;
  }

  recibir(): void {
    if (!this.seleccionado?.id || this.recepcion.invalid) return;
    const value = this.recepcion.getRawValue();
    this.guardando = true;
    this.api.PostApiV1ComprasIdRecepcion({ id: this.seleccionado.id }, {
      numeroFacturaProveedor: value.numeroFacturaProveedor.trim() || undefined,
      observaciones: value.observaciones.trim() || undefined,
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.recibirAbierto = false; this.inventario.marcarCambio(); this.toast.success('Recepción registrada. Stock, kardex y cuenta por pagar actualizados.'); this.cargar(); this.refrescarDashboard(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  abrirAnular(row: unknown): void {
    this.seleccionado = row as Api.CompraListadoDTO;
    this.anulacion.reset({ motivo: '' });
    this.anularAbierto = true;
  }

  anular(): void {
    this.anulacion.markAllAsTouched();
    if (!this.seleccionado?.id || this.anulacion.invalid) return;
    this.guardando = true;
    this.api.PostApiV1ComprasIdAnulacion({ id: this.seleccionado.id }, { motivo: this.anulacion.controls.motivo.value.trim() }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.anularAbierto = false; this.inventario.marcarCambio(); this.toast.success('Compra anulada.'); this.cargar(); this.refrescarDashboard(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  pedirEliminar(row: unknown): void { this.seleccionado = row as Api.CompraListadoDTO; this.confirmar = true; }

  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1ComprasId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.toast.success('Compra eliminada.'); this.cargar(); this.refrescarDashboard(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }

  agregarLinea(): void { this.detalles.push(this.linea()); }
  quitarLinea(index: number): void { if (this.detalles.length > 1) this.detalles.removeAt(index); }
  esBorrador(row: unknown): boolean { return (row as Api.CompraListadoDTO).estadoCompra === 'BORRADOR'; }
  puedeRecibir(row: unknown): boolean { const estado = (row as Api.CompraListadoDTO).estadoCompra; return estado === 'BORRADOR'; }
  puedeAnular(row: unknown): boolean { return (row as Api.CompraListadoDTO).estadoCompra !== 'ANULADA'; }
  errorDe(nombre: 'proveedorId' | 'sedeId' | 'fechaEmision' | 'numeroFacturaProveedor' | 'observaciones'): string | null { return fieldError(this.form.controls[nombre]); }
  errorMotivo(): string | null { return fieldError(this.anulacion.controls.motivo); }
  dash(key: string): string { const value = this.dashboard?.[key]; return value === undefined || value === null || value === '' ? '—' : String(value); }

  private linea(productoId = '', almacenId = '', cantidad = 1, precio = 0) {
    return this.fb.nonNullable.group({
      productoId: [productoId, Validators.required],
      almacenId: [almacenId, Validators.required],
      cantidad: [cantidad, [Validators.required, Validators.min(0.0001)]],
      precioUnitarioConIgv: [precio, [Validators.required, Validators.min(0)]],
    });
  }

  private vaciarLineas(): void {
    this.detalles.clear();
    this.detalles.push(this.linea());
  }

  private refrescarDashboard(): void {
    this.api.GetApiV1ComprasDashboard().pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => (this.dashboard = data),
      error: () => (this.dashboard = null),
    });
  }

  private filaDe(compra: Api.CompraResponseDTO): Api.CompraListadoDTO {
    return {
      id: compra.id,
      numeroOrdenCompra: compra.numeroOrdenCompra,
      numeroFacturaProveedor: compra.numeroFacturaProveedor,
      proveedorNombre: compra.proveedorNombre,
      fechaEmision: compra.fechaEmision,
      moneda: compra.moneda,
      total: compra.total,
      estadoCompra: compra.estadoCompra,
      estadoPago: compra.estadoPago,
      createdAt: compra.createdAt,
    };
  }

  private insertarFila(compra: Api.CompraResponseDTO): void {
    const filtros = this.filtros.getRawValue();
    const hayFiltro = Object.values(filtros).some((valor) => valor !== '');
    if (hayFiltro || this.state.page !== 0) {
      this.cargar();
      return;
    }
    const fila = this.filaDe(compra);
    this.state.rows = [fila, ...this.state.rows.filter((row) => row.id !== fila.id)].slice(0, this.state.size);
    this.state.totalElements += 1;
    this.state.totalPages = Math.max(1, Math.ceil(this.state.totalElements / this.state.size));
  }

  private reemplazarFila(compra: Api.CompraResponseDTO): void {
    const fila = this.filaDe(compra);
    const existe = this.state.rows.some((row) => row.id === fila.id);
    this.state.rows = existe
      ? this.state.rows.map((row) => row.id === fila.id ? fila : row)
      : this.state.rows;
    if (!existe) this.cargar();
  }
}

export const COMPRAS_ROUTES: Routes = [
  { path: '', component: ComprasPageComponent, title: 'Compras', data: { section: 'Compras', breadcrumb: 'Compras' } },
];
