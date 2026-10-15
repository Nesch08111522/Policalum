import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
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
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { DespachosApiService } from '../data-access/despachos.api';
import { MovilidadesApiService } from '../../movilidades/data-access/movilidades.api';

@Component({
  selector: 'app-despachos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, FormFieldComponent, SelectComponent, StatCardComponent],
  templateUrl: './despachos-page.component.html',
  styleUrls: ['./despachos-page.component.css'],
})
export class DespachosPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.OrdenDespachoResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'codigoSeguimiento', label: 'Seguimiento' },
    { key: 'clienteNombre', label: 'Cliente' },
    { key: 'ventaSerie', label: 'Venta' },
    { key: 'estado', label: 'Estado', kind: 'badge', badge: { PENDIENTE: 'warning', EN_CAMINO: 'info', ENTREGADO: 'success', FALLIDO: 'danger' } },
    { key: 'responsableNombre', label: 'Repartidor' },
    { key: 'fechaProgramada', label: 'Programada', kind: 'date' },
    { key: 'direccionEntrega', label: 'Dirección' },
  ];
  readonly filtros = this.fb.nonNullable.group({ sedeId: [''], estado: [''], responsableId: [''], desde: [''], hasta: [''] });
  readonly form = this.fb.nonNullable.group({
    ventaId: ['', Validators.required],
    sedeId: [''],
    direccionEntrega: ['', [Validators.required, Validators.maxLength(255)]],
    contactoNombre: ['', Validators.maxLength(120)],
    contactoTelefono: ['', Validators.maxLength(20)],
    fechaProgramada: [''],
    referenciaEntrega: ['', Validators.maxLength(120)],
    observaciones: ['', Validators.maxLength(500)],
  });
  readonly asignacion = this.fb.nonNullable.group({ responsableId: ['', Validators.required], movilidadId: [''] });
  readonly cambio = this.fb.nonNullable.group({
    estado: ['EN_CAMINO' as Api.CambiarEstadoDespachoRequestDTO['estado'], Validators.required],
    fechaProgramada: [''],
    motivo: [''],
  });
  sedes: SelectOption[] = [];
  usuarios: SelectOption[] = [];
  movilidades: SelectOption[] = [];
  dashboard: Api.DespachoDashboardDTO | null = null;
  modal = false;
  asignarAbierto = false;
  estadoAbierto = false;
  guardando = false;
  errorFormulario: string | null = null;
  seleccionado: Api.OrdenDespachoResponseDTO | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly api: DespachosApiService,
    private readonly movilidadesApi: MovilidadesApiService,
    private readonly lookup: LookupService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.api.GetApiV1DespachosDashboard().pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => (this.dashboard = data), error: () => (this.dashboard = null) });
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede')));
    this.lookup.loadUsuarios().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.usuarios = this.lookup.options(rows, (row) => `${(row as Api.UserResponseDTO).nombre ?? ''} ${(row as Api.UserResponseDTO).apellido ?? ''}`.trim() || 'Usuario')));
    this.movilidadesApi.GetApiV1Movilidades({ page: 0, size: 200 }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => (this.movilidades = (page.content ?? []).filter((row) => row.id).map((row) => ({ value: row.id ?? '', label: row.placa ?? 'Movilidad' }))),
      error: () => (this.movilidades = []),
    });
    this.cambio.controls.estado.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((estado) => {
      const motivo = this.cambio.controls.motivo;
      if (estado === 'FALLIDO') motivo.addValidators(Validators.required);
      else motivo.removeValidators(Validators.required);
      motivo.updateValueAndValidity();
    });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  cargar(): void {
    this.api.GetApiV1Despachos(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }

  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ sedeId: '', estado: '', responsableId: '', desde: '', hasta: '' }); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }

  abrirCrear(): void {
    this.errorFormulario = null;
    this.form.reset({ ventaId: '', sedeId: '', direccionEntrega: '', contactoNombre: '', contactoTelefono: '', fechaProgramada: '', referenciaEntrega: '', observaciones: '' });
    this.modal = true;
  }

  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.CrearDespachoRequestDTO = {
      ventaId: value.ventaId.trim(),
      sedeId: value.sedeId || undefined,
      direccionEntrega: value.direccionEntrega.trim(),
      contactoNombre: value.contactoNombre.trim() || undefined,
      contactoTelefono: value.contactoTelefono.trim() || undefined,
      fechaProgramada: value.fechaProgramada || undefined,
      referenciaEntrega: value.referenciaEntrega.trim() || undefined,
      observaciones: value.observaciones.trim() || undefined,
    };
    this.guardando = true;
    this.api.PostApiV1Despachos({}, body).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.toast.success('Despacho creado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }

  abrirAsignar(row: unknown): void {
    const item = row as Api.OrdenDespachoResponseDTO;
    this.seleccionado = item;
    this.asignacion.reset({ responsableId: item.responsableId ?? '', movilidadId: item.movilidadId ?? '' });
    this.asignarAbierto = true;
  }

  asignar(): void {
    this.asignacion.markAllAsTouched();
    if (!this.seleccionado?.id || this.asignacion.invalid) return;
    const value = this.asignacion.getRawValue();
    this.guardando = true;
    this.api.PostApiV1DespachosIdAsignarRepartidor({ id: this.seleccionado.id }, { responsableId: value.responsableId, movilidadId: value.movilidadId || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.asignarAbierto = false; this.toast.success('Repartidor asignado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  abrirEstado(row: unknown): void {
    const item = row as Api.OrdenDespachoResponseDTO;
    this.seleccionado = item;
    this.cambio.reset({ estado: item.estado && item.estado !== 'PENDIENTE' ? item.estado : 'EN_CAMINO', fechaProgramada: (item.fechaProgramada ?? '').slice(0, 10), motivo: '' });
    this.estadoAbierto = true;
  }

  cambiarEstado(): void {
    this.cambio.markAllAsTouched();
    if (!this.seleccionado?.id || this.cambio.invalid) return;
    const value = this.cambio.getRawValue();
    this.guardando = true;
    this.api.PatchApiV1DespachosIdEstado({ id: this.seleccionado.id }, {
      estado: value.estado,
      fechaProgramada: value.fechaProgramada || undefined,
      motivo: value.motivo.trim() || undefined,
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.estadoAbierto = false; this.toast.success('Estado actualizado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  errorDe(nombre: 'ventaId' | 'direccionEntrega'): string | null { return fieldError(this.form.controls[nombre]); }
}

export const DESPACHOS_ROUTES: Routes = [
  { path: '', component: DespachosPageComponent, title: 'Despachos', data: { section: 'Compras', breadcrumb: 'Despachos' } },
];
