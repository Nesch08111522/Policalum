import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, forkJoin, of, takeUntil } from 'rxjs';
import { catchError } from 'rxjs/operators';
import * as Api from '../../../core/api/openapi.types';
import { RoleGuard } from '../../../guards/role.guard';
import { BarChartComponent, ChartPoint } from '../../../shared/components/bar-chart/bar-chart.component';
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
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { PageState } from '../../../shared/utils/page-state';
import { FinanzasApiService } from '../data-access/finanzas.api';

function haceDias(dias: number): string {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() - dias);
  return fecha.toISOString().slice(0, 10);
}

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-finanzas-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, TabsComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, SelectComponent, StatCardComponent, BarChartComponent, PenPipe],
  templateUrl: './finanzas-page.component.html',
  styleUrls: ['./finanzas-page.component.css'],
})
export class FinanzasPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly tabs: TabItem[] = [
    { id: 'resumen', label: 'Resumen' },
    { id: 'cuentas', label: 'Cuentas' },
    { id: 'retiros', label: 'Retiros de socios' },
  ];
  tab = 'resumen';
  readonly cuentasState = new PageState<Api.CuentaFinancieraResponseDTO>();
  readonly retiros = new PageState<Api.RetiroSocioListadoDTO>();
  readonly columnasCuentas: ColumnDef[] = [
    { key: 'nombre', label: 'Cuenta' },
    { key: 'tipo', label: 'Tipo', kind: 'badge', badge: { EFECTIVO: 'success', BANCO: 'info', BILLETERA_DIGITAL: 'warning' } },
    { key: 'entidad', label: 'Entidad' },
    { key: 'sedeNombre', label: 'Sede' },
    { key: 'ultimoSaldo', label: 'Último saldo', kind: 'money', align: 'end' },
    { key: 'activa', label: 'Activa', kind: 'bool' },
  ];
  readonly columnasRetiros: ColumnDef[] = [
    { key: 'fechaRetiro', label: 'Fecha', kind: 'date' },
    { key: 'adminNombre', label: 'Socio' },
    { key: 'origenFondos', label: 'Origen', kind: 'badge' },
    { key: 'estado', label: 'Estado', kind: 'badge', badge: { REGISTRADO: 'info', ANULADO: 'danger' } },
    { key: 'sedeNombre', label: 'Sede' },
    { key: 'montoRetirado', label: 'Monto', kind: 'money', align: 'end' },
  ];
  readonly periodo = this.fb.nonNullable.group({ sedeId: [''], desde: [haceDias(30)], hasta: [new Date().toISOString().slice(0, 10)] });
  readonly filtrosRetiro = this.fb.nonNullable.group({ sedeId: [''], estado: [''], origen: [''], desde: [''], hasta: [''] });
  readonly cuenta = this.fb.nonNullable.group({
    id: [''],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    tipo: ['EFECTIVO' as Api.CuentaFinancieraRequestDTO['tipo'], Validators.required],
    entidad: [''],
    ultimosDigitos: ['', Validators.pattern(/^$|^\d{4}$/)],
    sedeId: [''],
  });
  readonly saldo = this.fb.nonNullable.group({ saldo: [0, [Validators.required, Validators.min(0)]], fechaCorte: [''], observacion: ['', Validators.maxLength(255)] });
  readonly retiro = this.fb.nonNullable.group({
    montoRetirado: [0, [Validators.required, Validators.min(0.01)]],
    origenFondos: ['CAJA_FISICA' as Api.RetiroSocioRequestDTO['origenFondos'], Validators.required],
    sedeId: [''],
    descripcion: ['', Validators.maxLength(255)],
  });
  readonly anularForm = this.fb.nonNullable.group({ motivo: ['', [Validators.required, Validators.maxLength(255)]] });
  sedes: SelectOption[] = [];
  kpis: Api.KpisFinancierosDTO | null = null;
  patrimonio: Api.PatrimonioResponseDTO | null = null;
  utilidad: Api.UtilidadNetaResponseDTO | null = null;
  puntos: ChartPoint[] = [];
  resumenError: string | null = null;
  resumenEstado: number | null = null;
  cargandoResumen = false;
  modalCuenta = false;
  modalSaldo = false;
  modalRetiro = false;
  modalAnular = false;
  confirmar = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  cuentaSeleccionada: Api.CuentaFinancieraResponseDTO | null = null;
  retiroSeleccionado: Api.RetiroSocioListadoDTO | null = null;
  private todas: Api.CuentaFinancieraResponseDTO[] = [];
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: FinanzasApiService, private readonly lookup: LookupService, private readonly toast: ToastService) {}

  ngOnInit(): void {
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede')));
    this.cargarResumen();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  cambiarTab(id: string): void {
    this.tab = id;
    if (id === 'resumen') this.cargarResumen();
    if (id === 'cuentas') this.cargarCuentas();
    if (id === 'retiros') this.cargarRetiros();
  }

  cargarResumen(): void {
    this.cargandoResumen = true;
    this.resumenError = null;
    const fechas = this.periodo.getRawValue();
    forkJoin({
      kpis: this.api.GetApiV1FinanzasDashboardKpis({ sedeId: fechas.sedeId, desde: fechas.desde, hasta: fechas.hasta }).pipe(catchError((error: unknown) => { this.resumenError = problemText(error); this.resumenEstado = problemStatus(error); return of(null); })),
      patrimonio: this.api.GetApiV1FinanzasDashboardPatrimonio({ sedeId: fechas.sedeId }).pipe(catchError(() => of(null))),
      utilidad: this.api.GetApiV1FinanzasUtilidadNeta({ sedeId: fechas.sedeId, desde: fechas.desde, hasta: fechas.hasta }).pipe(catchError(() => of(null))),
      evolucion: this.api.GetApiV1FinanzasDashboardEvolucionMensual({ sedeId: fechas.sedeId, meses: 6 }).pipe(catchError(() => of(null))),
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.kpis = data.kpis?.consolidado ?? null;
        this.patrimonio = data.patrimonio ?? null;
        this.utilidad = data.utilidad ?? null;
        this.puntos = (data.evolucion?.serie ?? []).filter((mes) => !!mes).map((mes) => ({
          label: mes.periodo ?? '',
          value: Number(mes.utilidadNeta ?? 0) || 0,
        }));
        this.cargandoResumen = false;
      },
      error: (error: unknown) => {
        this.cargandoResumen = false;
        this.resumenError = problemText(error);
        this.resumenEstado = problemStatus(error);
        this.kpis = null;
        this.patrimonio = null;
        this.utilidad = null;
        this.puntos = [];
      },
    });
  }

  cargarCuentas(): void {
    this.cuentasState.loading = true;
    this.api.GetApiV1FinanzasCuentas().pipe(takeUntil(this.destroy$)).subscribe({
      next: (rows) => { this.todas = rows ?? []; this.cuentasState.loading = false; this.paginarCuentas(); },
      error: (error: unknown) => this.cuentasState.fail(problemText(error), problemStatus(error)),
    });
  }

  paginarCuentas(): void {
    this.cuentasState.rows = this.todas;
    this.cuentasState.totalElements = this.todas.length;
    this.cuentasState.totalPages = this.todas.length ? 1 : 0;
    this.cuentasState.error = null;
  }

  cargarRetiros(): void {
    this.retiros.loading = true;
    this.api.GetApiV1FinanzasRetirosSocios(this.retiros.query(this.filtrosRetiro.getRawValue())).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => this.retiros.apply(page),
      error: (error: unknown) => this.retiros.fail(problemText(error), problemStatus(error)),
    });
  }

  filtrarRetiros(): void { this.retiros.page = 0; this.cargarRetiros(); }
  limpiarRetiros(): void { this.filtrosRetiro.reset({ sedeId: '', estado: '', origen: '', desde: '', hasta: '' }); this.filtrarRetiros(); }
  cambiarPagina(page: number): void { this.retiros.page = page; this.cargarRetiros(); }
  cambiarTamano(size: number): void { this.retiros.size = size; this.retiros.page = 0; this.cargarRetiros(); }

  abrirCuenta(): void {
    this.editando = false;
    this.errorFormulario = null;
    this.cuenta.reset({ id: '', nombre: '', tipo: 'EFECTIVO', entidad: '', ultimosDigitos: '', sedeId: '' });
    this.modalCuenta = true;
  }

  editarCuenta(row: unknown): void {
    const item = row as Api.CuentaFinancieraResponseDTO;
    this.editando = true;
    this.cuenta.reset({ id: item.id ?? '', nombre: item.nombre ?? '', tipo: item.tipo ?? 'EFECTIVO', entidad: item.entidad ?? '', ultimosDigitos: item.ultimosDigitos ?? '', sedeId: item.sedeId ?? '' });
    this.modalCuenta = true;
  }

  guardarCuenta(): void {
    this.cuenta.markAllAsTouched();
    if (this.cuenta.invalid) return;
    const value = this.cuenta.getRawValue();
    const body: Api.CuentaFinancieraRequestDTO = {
      nombre: value.nombre.trim(),
      tipo: value.tipo,
      entidad: value.entidad.trim() || undefined,
      ultimosDigitos: value.ultimosDigitos.trim() || undefined,
      sedeId: value.sedeId || undefined,
    };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1FinanzasCuentasId({ id: value.id }, body) : this.api.PostApiV1FinanzasCuentas({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modalCuenta = false; this.toast.success('Cuenta guardada.'); this.cargarCuentas(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.cuenta, error); this.errorFormulario = problemText(error); },
    });
  }

  pedirEliminar(row: unknown): void { this.cuentaSeleccionada = row as Api.CuentaFinancieraResponseDTO; this.confirmar = true; }
  eliminarCuenta(): void {
    if (!this.cuentaSeleccionada?.id) return;
    this.guardando = true;
    this.api.DeleteApiV1FinanzasCuentasId({ id: this.cuentaSeleccionada.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.confirmar = false; this.toast.success('Cuenta eliminada.'); this.cargarCuentas(); },
      error: (error: unknown) => { this.guardando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }

  abrirSaldo(row: unknown): void {
    this.cuentaSeleccionada = row as Api.CuentaFinancieraResponseDTO;
    this.saldo.reset({ saldo: this.cuentaSeleccionada.ultimoSaldo ?? 0, fechaCorte: '', observacion: '' });
    this.modalSaldo = true;
  }

  guardarSaldo(): void {
    this.saldo.markAllAsTouched();
    if (!this.cuentaSeleccionada?.id || this.saldo.invalid) return;
    const value = this.saldo.getRawValue();
    this.guardando = true;
    this.api.PostApiV1FinanzasCuentasIdSaldos({ id: this.cuentaSeleccionada.id }, { saldo: Number(value.saldo), fechaCorte: value.fechaCorte || undefined, observacion: value.observacion.trim() || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modalSaldo = false; this.toast.success('Saldo registrado.'); this.cargarCuentas(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  abrirRetiro(): void { this.retiro.reset({ montoRetirado: 0, origenFondos: 'CAJA_FISICA', sedeId: '', descripcion: '' }); this.modalRetiro = true; }
  guardarRetiro(): void {
    this.retiro.markAllAsTouched();
    if (this.retiro.invalid) return;
    const value = this.retiro.getRawValue();
    this.guardando = true;
    this.api.PostApiV1FinanzasRetirosSocios({}, { montoRetirado: Number(value.montoRetirado), origenFondos: value.origenFondos, sedeId: value.sedeId || undefined, descripcion: value.descripcion.trim() || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modalRetiro = false; this.toast.success('Retiro registrado.'); this.cargarRetiros(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  abrirAnular(row: unknown): void { this.retiroSeleccionado = row as Api.RetiroSocioListadoDTO; this.anularForm.reset({ motivo: '' }); this.modalAnular = true; }
  puedeAnular(row: unknown): boolean { return (row as Api.RetiroSocioListadoDTO).estado === 'REGISTRADO'; }
  anularRetiro(): void {
    this.anularForm.markAllAsTouched();
    if (!this.retiroSeleccionado?.id || this.anularForm.invalid) return;
    this.guardando = true;
    this.api.PostApiV1FinanzasRetirosSociosIdAnular({ id: this.retiroSeleccionado.id }, { motivo: this.anularForm.controls.motivo.value.trim() }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modalAnular = false; this.toast.success('Retiro anulado.'); this.cargarRetiros(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  errorCuenta(nombre: 'nombre' | 'ultimosDigitos'): string | null { return fieldError(this.cuenta.controls[nombre], { pattern: 'Usa exactamente 4 dígitos.' }); }
}

export const FINANZAS_ROUTES: Routes = [{
  path: '',
  component: FinanzasPageComponent,
  canActivate: [RoleGuard],
  title: 'Finanzas',
  data: { expectedRoles: ['ROLE_ADMIN'], section: 'Finanzas', breadcrumb: 'Finanzas' },
}];
