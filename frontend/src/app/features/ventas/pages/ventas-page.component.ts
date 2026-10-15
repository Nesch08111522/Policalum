import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink, Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { ColumnDef, SelectOption } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { lineaProductoOServicio } from '../../../shared/utils/validators';
import { VentasApiService } from '../data-access/ventas.api';

const TURNO_KEY = 'erp-turno-caja';

@Component({
  selector: 'app-ventas-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, FormFieldComponent, SelectComponent, PenPipe],
  templateUrl: './ventas-page.component.html',
  styleUrls: ['./ventas-page.component.css'],
})
export class VentasPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
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
  modal = false;
  guardando = false;
  errorFormulario: string | null = null;
  detalle: Api.VentaDetalleResponseDTO | null = null;
  detalleAbierto = false;
  anularAbierto = false;
  seleccionado: Api.VentaListadoResponseDTO | null = null;
  private readonly destroy$ = new Subject<void>();

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
    this.api.GetApiV1Ventas(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
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
    this.lineas.push(this.linea());
    this.pagos.push(this.pago());
    this.modal = true;
  }

  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
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
        servicioId: linea.servicioId || undefined,
      })),
      pagos: value.pagos.map((pago) => ({ metodoPago: pago.metodoPago, monto: Number(pago.monto), referencia: pago.referencia.trim() || undefined })),
    };
    this.guardando = true;
    this.errorFormulario = null;
    this.api.PostApiV1Ventas({}, body).pipe(takeUntil(this.destroy$)).subscribe({
      next: (venta) => { this.guardando = false; this.modal = false; this.toast.success(`Venta ${venta.serieCorrelativo ?? ''} registrada.`.trim()); this.cargar(); },
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
      next: () => { this.guardando = false; this.anularAbierto = false; this.toast.success('Venta anulada.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  agregarLinea(): void { this.lineas.push(this.linea()); }
  quitarLinea(index: number): void { if (this.lineas.length > 1) this.lineas.removeAt(index); }
  agregarPago(): void { this.pagos.push(this.pago()); }
  quitarPago(index: number): void { if (this.pagos.length > 1) this.pagos.removeAt(index); }
  errorDe(nombre: 'sedeId' | 'almacenId' | 'turnoCajaId' | 'tipoComprobante'): string | null { return fieldError(this.form.controls[nombre]); }
  errorMotivo(): string | null { return fieldError(this.anulacion.controls.motivo); }

  private linea() {
    return this.fb.nonNullable.group({
      productoId: [''],
      servicioId: [''],
      cantidad: [1, [Validators.required, Validators.min(0.0001)]],
    }, { validators: lineaProductoOServicio });
  }

  private pago() {
    return this.fb.nonNullable.group({
      metodoPago: ['EFECTIVO' as Api.PagoVentaCommandDTO['metodoPago'], Validators.required],
      monto: [0, [Validators.required, Validators.min(0.01)]],
      referencia: [''],
    });
  }

  private turnoActivo(): string {
    return typeof sessionStorage === 'undefined' ? '' : sessionStorage.getItem(TURNO_KEY) ?? '';
  }
}

export const VENTAS_ROUTES: Routes = [
  { path: '', component: VentasPageComponent, title: 'Ventas', data: { section: 'Ventas', breadcrumb: 'Ventas' } },
];
