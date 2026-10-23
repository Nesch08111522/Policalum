import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
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
import { ColumnDef, SelectOption } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { CajaSessionService } from '../../../shared/services/caja-session.service';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { DevolucionesApiService } from '../../devoluciones/data-access/devoluciones.api';
import { ReembolsosApiService } from '../data-access/reembolsos.api';

@Component({
  selector: 'app-reembolsos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, FormFieldComponent, SelectComponent, PenPipe],
  templateUrl: './reembolsos-page.component.html',
  styleUrls: ['./reembolsos-page.component.css'],
})
export class ReembolsosPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly caja = inject(CajaSessionService);
  readonly admin = inject(AuthService).isAdmin();
  readonly state = new PageState<Api.ReembolsoResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'devolucionCodigo', label: 'Devolución' },
    { key: 'clienteNombre', label: 'Cliente' },
    { key: 'metodoReembolso', label: 'Método', kind: 'badge', badge: { EFECTIVO: 'success', YAPE: 'info', PLIN: 'info', TARJETA: 'neutral', TRANSFERENCIA: 'warning' } },
    { key: 'estado', label: 'Estado', kind: 'badge', badge: { PENDIENTE: 'warning', PROCESADO: 'success' } },
    { key: 'createdAt', label: 'Registro', kind: 'date', sortable: true },
    { key: 'montoTotalReembolso', label: 'Monto', kind: 'money', align: 'end' },
  ];
  readonly filtros = this.fb.nonNullable.group({ sedeId: [''], estado: [''], metodo: [''], desde: [''], hasta: [''] });
  readonly form = this.fb.nonNullable.group({
    devolucionId: ['', Validators.required],
    metodoReembolso: ['EFECTIVO' as Api.ReembolsoRequestDTO['metodoReembolso'], Validators.required],
    monto: ['', Validators.min(0.01)],
    motivoAjuste: [''],
    observaciones: ['', Validators.maxLength(500)],
    referencia: [''],
  });
  readonly proceso = this.fb.nonNullable.group({
    observaciones: ['', Validators.maxLength(500)],
    referencia: [''],
  });
  sedes: SelectOption[] = [];
  devoluciones: SelectOption[] = [];
  modal = false;
  procesarAbierto = false;
  guardando = false;
  errorFormulario: string | null = null;
  detalle: Api.ReembolsoResponseDTO | null = null;
  detalleAbierto = false;
  seleccionado: Api.ReembolsoResponseDTO | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly api: ReembolsosApiService,
    private readonly devolucionesApi: DevolucionesApiService,
    private readonly lookup: LookupService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => {
      this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede');
    });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  cargar(): void {
    this.api.GetApiV1Reembolsos(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => { this.state.apply(page); this.pintar(); },
      error: (error: unknown) => { this.state.fail(problemText(error), problemStatus(error)); this.pintar(); },
    });
  }

  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ sedeId: '', estado: '', metodo: '', desde: '', hasta: '' }); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }

  abrirCrear(): void {
    this.errorFormulario = null;
    this.form.reset({ devolucionId: '', metodoReembolso: 'EFECTIVO', monto: '', motivoAjuste: '', observaciones: '', referencia: '' });
    this.modal = true;
    this.devolucionesApi.GetApiV1Devoluciones({ estado: 'APROBADA', page: 0, size: 50, sort: 'fechaSolicitud,desc' }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => {
        this.devoluciones = (page.content ?? []).filter((row) => row.id).map((row) => ({
          value: row.id ?? '',
          label: `${row.codigo ?? 'Devolución'} · ${row.clienteNombre || 'Sin cliente'} · S/ ${row.montoTotal ?? 0}`,
        }));
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const textoMonto = String(value.monto ?? '').trim();
    const monto = textoMonto ? Number(textoMonto) : undefined;
    const body: Api.ReembolsoRequestDTO = {
      devolucionId: value.devolucionId,
      metodoReembolso: value.metodoReembolso,
      monto,
      motivoAjuste: value.motivoAjuste.trim() || undefined,
      observaciones: value.observaciones.trim() || undefined,
      referencia: value.referencia.trim() || undefined,
    };
    this.guardando = true;
    this.errorFormulario = null;
    this.api.PostApiV1Reembolsos({}, body).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.toast.success('Reembolso registrado. Queda pendiente de pago.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }

  ver(row: unknown): void {
    const item = row as Api.ReembolsoResponseDTO;
    if (!item.id) return;
    this.api.GetApiV1ReembolsosId({ id: item.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => { this.detalle = data; this.detalleAbierto = true; },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  abrirProcesar(row: unknown): void {
    this.seleccionado = row as Api.ReembolsoResponseDTO;
    this.proceso.reset({ observaciones: '', referencia: '' });
    this.procesarAbierto = true;
  }

  pendiente(row: unknown): boolean { return (row as Api.ReembolsoResponseDTO).estado === 'PENDIENTE'; }

  procesar(): void {
    if (this.guardando || !this.seleccionado?.id) return;
    this.guardando = true;
    const value = this.proceso.getRawValue();
    this.api.PostApiV1ReembolsosIdProcesar({ id: this.seleccionado.id }, {
      observaciones: value.observaciones.trim() || undefined,
      referencia: value.referencia.trim() || undefined,
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.guardando = false;
        this.procesarAbierto = false;
        this.caja.tocar();
        this.toast.success('Reembolso procesado.');
        this.cargar();
      },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  errorDe(nombre: 'devolucionId' | 'monto' | 'motivoAjuste' | 'observaciones'): string | null { return fieldError(this.form.controls[nombre]); }

  /** El fetch de Angular 22 no agenda detección de cambios; sin esto la tabla se queda en el esqueleto. */
  private pintar(): void { this.cdr.detectChanges(); }
}

export const REEMBOLSOS_ROUTES: Routes = [
  { path: '', component: ReembolsosPageComponent, title: 'Reembolsos', data: { section: 'Ventas', breadcrumb: 'Reembolsos' } },
];
