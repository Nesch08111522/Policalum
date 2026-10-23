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
import { ColumnDef, SelectOption } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { CuentasPorPagarApiService } from '../data-access/cuentas-por-pagar.api';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-cuentas-por-pagar-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, FormFieldComponent, SelectComponent, StatCardComponent, PenPipe],
  templateUrl: './cuentas-por-pagar-page.component.html',
  styleUrls: ['./cuentas-por-pagar-page.component.css'],
})
export class CuentasPorPagarPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.CuentaPorPagarResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'proveedorNombre', label: 'Proveedor' },
    { key: 'compraNumeroOC', label: 'Orden' },
    { key: 'fechaVencimiento', label: 'Vence', kind: 'date' },
    { key: 'montoTotal', label: 'Total', kind: 'money', align: 'end' },
    { key: 'saldoPendiente', label: 'Saldo', kind: 'money', align: 'end' },
    { key: 'estado', label: 'Estado', kind: 'badge', badge: { PENDIENTE: 'warning', PAGADO_PARCIAL: 'info', PAGADO: 'success' } },
    { key: 'vencida', label: 'Vencida', kind: 'bool' },
  ];
  readonly filtros = this.fb.nonNullable.group({ sedeId: [''], proveedorId: [''], estado: [''], soloVencidas: [''] });
  readonly pago = this.fb.nonNullable.group({
    monto: [0, [Validators.required, Validators.min(0.01)]],
    metodoPago: ['EFECTIVO' as Api.PagoCxPRequestDTO['metodoPago'], Validators.required],
    referencia: [''], observaciones: [''],
  });
  resumen: Api.ResumenCxPDashboardDTO | null = null;
  sedes: SelectOption[] = [];
  proveedores: SelectOption[] = [];
  modal = false;
  guardando = false;
  errorFormulario: string | null = null;
  seleccionado: Api.CuentaPorPagarResponseDTO | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: CuentasPorPagarApiService, private readonly lookup: LookupService, private readonly toast: ToastService) {}
  ngOnInit(): void {
    this.cargar();
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede'); });
    this.lookup.loadProveedores().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.proveedores = this.lookup.options(rows, (row) => (row as Api.ProveedorResponseDTO).nombreRazon ?? 'Proveedor'); });
    this.api.GetApiV1CuentasPorPagarDashboard().pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => { this.resumen = data; }, error: () => { this.resumen = null; } });
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  cargar(): void {
    this.api.GetApiV1CuentasPorPagar(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }
  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset(); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  abrirPago(row: unknown): void {
    const cuenta = row as Api.CuentaPorPagarResponseDTO;
    this.seleccionado = cuenta;
    this.errorFormulario = null;
    this.pago.reset({ monto: cuenta.saldoPendiente ?? 0, metodoPago: 'EFECTIVO', referencia: '', observaciones: '' });
    this.modal = true;
  }
  pagar(): void {
    this.pago.markAllAsTouched();
    if (this.pago.invalid || !this.seleccionado?.id) return;
    const value = this.pago.getRawValue();
    this.guardando = true;
    this.api.PostApiV1CuentasPorPagarIdPago({ id: this.seleccionado.id }, { monto: Number(value.monto), metodoPago: value.metodoPago, referencia: value.referencia.trim() || undefined, observaciones: value.observaciones.trim() || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.toast.success('Pago registrado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.errorFormulario = problemText(error); },
    });
  }
}

export const CUENTAS_POR_PAGAR_ROUTES: Routes = [
  { path: '', component: CuentasPorPagarPageComponent, title: 'Cuentas por pagar', data: { section: 'Finanzas', breadcrumb: 'Cuentas por pagar' } },
];
