import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink, Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { ExportButtonsComponent } from '../../../shared/components/export-buttons/export-buttons.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { ColumnDef, SelectOption } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { CajaSessionService } from '../../../shared/services/caja-session.service';
import { InventarioSyncService } from '../../../shared/services/inventario-sync.service';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { rangoFechaFiltro } from '../../../shared/utils/turno';
import { lineaProductoOServicio } from '../../../shared/utils/validators';
import { CatalogoFraccionesApiService } from '../../catalogo-fracciones/data-access/catalogo-fracciones.api';
import { VentasApiService } from '../data-access/ventas.api';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-ventas-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PageHeaderComponent, ExportButtonsComponent, FilterBarComponent, DataTableComponent, ModalComponent, FormFieldComponent, SelectComponent, PenPipe],
  templateUrl: './ventas-page.component.html',
  styleUrls: ['./ventas-page.component.css'],
})
export class VentasPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly caja = inject(CajaSessionService);
  private readonly inventario = inject(InventarioSyncService);
  private readonly fraccionesApi = inject(CatalogoFraccionesApiService);
  private readonly destroy$ = new Subject<void>();
  private readonly precioFraccion = new Map<string, number>();
  readonly state = new PageState<Api.VentaListadoResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'serieCorrelativo', label: 'Comprobante', sortable: true },
    { key: 'clienteNombre', label: 'Cliente' },
    { key: 'tipoComprobante', label: 'Tipo', kind: 'badge', badge: { BOLETA: 'info', FACTURA: 'success', TICKET: 'neutral' } },
    { key: 'canalVenta', label: 'Canal', kind: 'badge', badge: { LOCAL: 'info', ONLINE: 'warning' } },
    { key: 'estado', label: 'Estado', kind: 'badge', badge: { COMPLETADA: 'success', ANULADA: 'danger' } },
    { key: 'createdAt', label: 'Fecha', kind: 'date', sortable: true },
    { key: 'total', label: 'Total', kind: 'money', align: 'end' },
  ];
  readonly filtros = this.fb.nonNullable.group({ sedeId: [''], clienteId: [''], estado: [''], canal: [''], desde: [''], hasta: [''] });
  readonly form = this.fb.nonNullable.group({
    sedeId: ['', Validators.required],
    almacenId: ['', Validators.required],
    turnoCajaId: [this.turnoActivo(), Validators.required],
    clienteId: [''],
    tipoComprobante: ['BOLETA' as Api.RegistrarVentaRequestDTO['tipoComprobante'], Validators.required],
    canalVenta: ['LOCAL' as NonNullable<Api.RegistrarVentaRequestDTO['canalVenta']>],
    lineas: this.fb.array([this.linea()]),
    pagos: this.fb.array([this.pago()]),
  });
  readonly anulacion = this.fb.nonNullable.group({ motivo: ['', [Validators.required, Validators.maxLength(255)]] });
  sedes: SelectOption[] = [];
  almacenes: SelectOption[] = [];
  clientes: SelectOption[] = [];
  productos: SelectOption[] = [];
  servicios: SelectOption[] = [];
  fraccionesLinea: SelectOption[][] = [];
  totalEstimado = 0;
  totalCompleto = true;
  modal = false;
  guardando = false;
  errorFormulario: string | null = null;
  detalle: Api.VentaDetalleResponseDTO | null = null;
  detalleAbierto = false;
  anularAbierto = false;
  seleccionado: Api.VentaListadoResponseDTO | null = null;

  constructor(private readonly api: VentasApiService, private readonly lookup: LookupService, private readonly toast: ToastService) {
    this.state.sort = 'createdAt,desc';
  }

  ngOnInit(): void {
    this.cargar();
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede')));
    this.lookup.loadAlmacenes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.almacenes = this.lookup.options(rows, (row) => (row as Api.AlmacenResponseDTO).nombre ?? 'Almacén')));
    this.lookup.loadClientes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.clientes = this.lookup.options(rows, (row) => (row as Api.ClienteResponseDTO).nombre ?? 'Cliente')));
    this.lookup.loadProductos().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.productos = this.lookup.options(rows, (row) => (row as Api.ProductoResponseDTO).nombre ?? 'Producto')));
    this.lookup.loadServicios().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.servicios = this.lookup.options(rows, (row) => (row as Api.ServicioResponseDTO).nombre ?? 'Servicio')));
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  get lineas() { return this.form.controls.lineas; }
  get pagos() { return this.form.controls.pagos; }

  cargar(): void {
    this.api.GetApiV1Ventas(this.state.query(rangoFechaFiltro(this.filtros.getRawValue()))).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }

  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ sedeId: '', clienteId: '', estado: '', canal: '', desde: '', hasta: '' }); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }

  abrirCrear(): void {
    this.errorFormulario = null;
    this.form.reset({ sedeId: '', almacenId: '', turnoCajaId: this.turnoActivo(), clienteId: '', tipoComprobante: 'BOLETA', canalVenta: 'LOCAL' });
    this.lineas.clear();
    this.pagos.clear();
    this.fraccionesLinea = [];
    this.lineas.push(this.linea());
    this.pagos.push(this.pago());
    this.recalcularTotal();
    this.modal = true;
  }

  guardar(): void {
    this.errorFormulario = null;
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    if (!value.turnoCajaId.trim()) {
      this.errorFormulario = 'Para registrar una venta abre la caja de esta sede.';
      return;
    }
    const pagos = this.pagosAEnviar(value.pagos);
    if (!pagos) return;
    const body: Api.RegistrarVentaRequestDTO = {
      sedeId: value.sedeId,
      almacenId: value.almacenId,
      turnoCajaId: value.turnoCajaId.trim(),
      tipoComprobante: value.tipoComprobante,
      canalVenta: value.canalVenta,
      clienteId: value.clienteId || undefined,
      lineas: value.lineas.map((linea) => ({
        cantidad: Number(linea.cantidad),
        productoId: linea.productoId || undefined,
        fraccionId: linea.fraccionId || undefined,
        servicioId: linea.servicioId || undefined,
      })),
      pagos,
    };
    this.guardando = true;
    this.api.PostApiV1Ventas({}, body).pipe(takeUntil(this.destroy$)).subscribe({
      next: (venta) => { this.guardando = false; this.modal = false; this.inventario.marcarCambio(); this.caja.tocar(); this.lookup.invalidate('productos'); this.toast.success(`Venta ${venta.serieCorrelativo ?? ''} registrada.`.trim()); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }

  ver(row: unknown): void {
    const venta = row as Api.VentaListadoResponseDTO;
    if (!venta.id) return;
    this.api.GetApiV1VentasId({ id: venta.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => { this.detalle = data; this.detalleAbierto = true; },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  abrirAnular(row: unknown): void { this.seleccionado = row as Api.VentaListadoResponseDTO; this.anulacion.reset({ motivo: '' }); this.anularAbierto = true; }
  puedeAnular(row: unknown): boolean { return (row as Api.VentaListadoResponseDTO).estado === 'COMPLETADA'; }

  anular(): void {
    this.anulacion.markAllAsTouched();
    if (!this.seleccionado?.id || this.anulacion.invalid) return;
    this.guardando = true;
    this.api.PostApiV1VentasIdAnulacion({ id: this.seleccionado.id }, { motivo: this.anulacion.controls.motivo.value.trim() }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.anularAbierto = false; this.inventario.marcarCambio(); this.caja.tocar(); this.toast.success('Venta anulada.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  agregarLinea(): void { this.lineas.push(this.linea()); this.fraccionesLinea.push([]); }
  quitarLinea(index: number): void { if (this.lineas.length > 1) { this.lineas.removeAt(index); this.fraccionesLinea.splice(index, 1); this.recalcularTotal(); } }
  usarTotal(): void { if (this.totalCompleto && this.pagos.length) this.pagos.at(0).controls.monto.setValue(this.totalEstimado); }
  agregarPago(): void { this.pagos.push(this.pago()); }
  quitarPago(index: number): void { if (this.pagos.length > 1) this.pagos.removeAt(index); }
  errorDe(nombre: 'sedeId' | 'almacenId' | 'turnoCajaId' | 'tipoComprobante'): string | null { return fieldError(this.form.controls[nombre]); }
  errorMotivo(): string | null { return fieldError(this.anulacion.controls.motivo); }

  private linea() {
    const group = this.fb.nonNullable.group({
      productoId: [''],
      fraccionId: [''],
      servicioId: [''],
      cantidad: [1, [Validators.required, Validators.min(0.0001)]],
    }, { validators: lineaProductoOServicio });
    group.controls.productoId.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.cargarFracciones(group));
    group.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.recalcularTotal());
    return group;
  }

  private cargarFracciones(group: ReturnType<VentasPageComponent['linea']>): void {
    const productoId = group.controls.productoId.value;
    const producto = this.lookup.productos.find((item) => item.id === productoId);
    group.controls.fraccionId.setValue('', { emitEvent: false });
    const index = this.lineas.controls.indexOf(group);
    if (!productoId || !producto?.esFraccionable) {
      group.controls.fraccionId.clearValidators();
      group.controls.fraccionId.updateValueAndValidity({ emitEvent: false });
      if (index >= 0) this.fraccionesLinea[index] = [];
      this.recalcularTotal();
      return;
    }
    group.controls.fraccionId.addValidators(Validators.required);
    group.controls.fraccionId.updateValueAndValidity({ emitEvent: false });
    this.fraccionesApi.GetApiV1CatalogoFraccionesProductoProductoIdDisponibles({ productoId }).pipe(takeUntil(this.destroy$)).subscribe((rows) => {
      for (const row of rows ?? []) if (row.id != null) this.precioFraccion.set(row.id, Number(row.precioVenta ?? 0));
      const current = this.lineas.controls.indexOf(group);
      if (current >= 0) this.fraccionesLinea[current] = (rows ?? []).filter((row) => row.id).map((row) => ({ value: row.id as string, label: row.nombreFraccion ?? 'Fracción' }));
      this.recalcularTotal();
    });
  }

  private recalcularTotal(): void {
    let total = 0;
    let completo = true;
    for (const linea of this.lineas.controls) {
      const value = linea.getRawValue();
      if (!value.productoId && !value.servicioId) continue;
      const precio = this.precioLinea(value.productoId, value.fraccionId, value.servicioId);
      if (precio == null) { completo = false; continue; }
      total += Math.round(precio * Number(value.cantidad) * 100) / 100;
    }
    this.totalEstimado = Math.round(total * 100) / 100;
    this.totalCompleto = completo;
  }

  private precioLinea(productoId: string, fraccionId: string, servicioId: string): number | null {
    if (servicioId) {
      const servicio = this.lookup.servicios.find((item) => item.id === servicioId);
      return servicio?.precio == null ? null : Number(servicio.precio);
    }
    const producto = this.lookup.productos.find((item) => item.id === productoId);
    if (!producto) return null;
    if (producto.esFraccionable) return fraccionId && this.precioFraccion.has(fraccionId) ? this.precioFraccion.get(fraccionId) ?? null : null;
    return producto.precioVentaBase == null ? null : Number(producto.precioVentaBase);
  }

  private pagosAEnviar(pagos: Array<{ metodoPago: Api.PagoVentaCommandDTO['metodoPago']; monto: number; referencia: string }>): Api.RegistrarVentaRequestDTO['pagos'] | null {
    this.recalcularTotal();
    if (!this.totalCompleto) {
      this.errorFormulario = 'Selecciona la fracción de cada producto fraccionable.';
      return null;
    }
    const enviados = pagos.map((pago) => ({ metodoPago: pago.metodoPago, monto: Number(pago.monto), referencia: pago.referencia.trim() || undefined }));
    const suma = Math.round(enviados.reduce((acc, pago) => acc + pago.monto, 0) * 100) / 100;
    if (enviados.length === 1 && suma === 0) {
      enviados[0].monto = this.totalEstimado;
      this.pagos.at(0).controls.monto.setValue(this.totalEstimado, { emitEvent: false });
      return enviados;
    }
    if (Math.round(suma * 100) !== Math.round(this.totalEstimado * 100)) {
      this.errorFormulario = `La suma de pagos (${suma.toFixed(2)}) debe coincidir con el total (${this.totalEstimado.toFixed(2)}).`;
      return null;
    }
    return enviados;
  }

  private pago() {
    return this.fb.nonNullable.group({
      metodoPago: ['EFECTIVO' as Api.PagoVentaCommandDTO['metodoPago'], Validators.required],
      monto: [0, [Validators.required, Validators.min(0.01)]],
      referencia: [''],
    });
  }

  private turnoActivo(): string {
    return this.caja.id();
  }

  get cajaAbierta(): boolean {
    return !!this.caja.id();
  }
}

export const VENTAS_ROUTES: Routes = [
  { path: '', component: VentasPageComponent, title: 'Ventas', data: { section: 'Ventas', breadcrumb: 'Ventas' } },
];
