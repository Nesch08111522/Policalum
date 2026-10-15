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
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { ColumnDef, SelectOption } from '../../../shared/models/ui.models';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { MovilidadesApiService } from '../data-access/movilidades.api';

@Component({
  selector: 'app-movilidades-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, SelectComponent, StatCardComponent],
  templateUrl: './movilidades-page.component.html',
  styleUrls: ['./movilidades-page.component.css'],
})
export class MovilidadesPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.MovilidadListadoDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'placa', label: 'Placa', sortable: true },
    { key: 'tipo', label: 'Tipo', kind: 'badge', badge: { MOTO: 'info', AUTO: 'neutral', CAMIONETA: 'warning', CAMION: 'success' } },
    { key: 'marca', label: 'Marca' },
    { key: 'modelo', label: 'Modelo' },
    { key: 'conductorNombre', label: 'Conductor' },
    { key: 'sedeNombre', label: 'Sede' },
    { key: 'activo', label: 'Activo', kind: 'bool' },
  ];
  readonly filtros = this.fb.nonNullable.group({ texto: [''], sedeId: [''], activo: [''], tipo: [''] });
  readonly form = this.fb.nonNullable.group({
    id: [''], placa: ['', [Validators.required, Validators.maxLength(10)]], tipo: ['MOTO' as Api.MovilidadRequestDTO['tipo'], Validators.required],
    marca: [''], modelo: [''], sedeId: ['', Validators.required], activo: [true],
  });
  readonly conductorForm = this.fb.nonNullable.group({ conductorId: [''] });
  sedes: SelectOption[] = [];
  usuarios: SelectOption[] = [];
  dashboard: Api.MovilidadDashboardDTO | null = null;
  modal = false;
  conductorModal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.MovilidadListadoDTO | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: MovilidadesApiService, private readonly lookup: LookupService, private readonly toast: ToastService) { this.state.sort = 'placa,asc'; }
  ngOnInit(): void {
    this.cargar();
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede'); });
    this.lookup.loadUsuarios().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.usuarios = rows.filter((row) => row.id).map((row) => ({ value: row.id ?? '', label: `${row.nombre ?? ''} ${row.apellido ?? ''}`.trim() })); });
    this.api.GetApiV1MovilidadesDashboard().pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => { this.dashboard = data; }, error: () => { this.dashboard = null; } });
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  cargar(): void {
    this.api.GetApiV1Movilidades(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }
  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ texto: '', sedeId: '', activo: '', tipo: '' }); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  abrirCrear(): void { this.editando = false; this.errorFormulario = null; this.form.reset({ id: '', placa: '', tipo: 'MOTO', marca: '', modelo: '', sedeId: '', activo: true }); this.modal = true; }
  abrirEditar(row: unknown): void {
    const item = row as Api.MovilidadListadoDTO;
    this.api.GetApiV1MovilidadesId({ id: item.id ?? '' }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (detalle) => {
        this.editando = true;
        this.errorFormulario = null;
        this.form.reset({ id: detalle.id ?? '', placa: detalle.placa ?? '', tipo: (detalle.tipo as Api.MovilidadRequestDTO['tipo']) ?? 'MOTO', marca: detalle.marca ?? '', modelo: detalle.modelo ?? '', sedeId: detalle.sedeId ?? '', activo: detalle.activo ?? true });
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
    const body: Api.MovilidadRequestDTO = { placa: value.placa.trim().toUpperCase(), tipo: value.tipo, marca: value.marca.trim() || undefined, modelo: value.modelo.trim() || undefined, sedeId: value.sedeId, activo: value.activo };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1MovilidadesId({ id: value.id }, body) : this.api.PostApiV1Movilidades({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.toast.success(this.editando ? 'Movilidad actualizada.' : 'Movilidad creada.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }
  abrirConductor(row: unknown): void { this.seleccionado = row as Api.MovilidadListadoDTO; this.conductorForm.reset({ conductorId: '' }); this.conductorModal = true; }
  asignarConductor(): void {
    if (!this.seleccionado?.id) return;
    this.guardando = true;
    this.api.PostApiV1MovilidadesIdAsignarConductor({ id: this.seleccionado.id }, { conductorId: this.conductorForm.controls.conductorId.value || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.conductorModal = false; this.toast.success('Conductor asignado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }
  pedirEliminar(row: unknown): void { this.seleccionado = row as Api.MovilidadListadoDTO; this.confirmar = true; }
  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1MovilidadesId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.toast.success('Movilidad eliminada.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }
  errorDe(nombre: 'placa' | 'sedeId'): string | null { return fieldError(this.form.controls[nombre]); }
}

export const MOVILIDADES_ROUTES: Routes = [
  { path: '', component: MovilidadesPageComponent, title: 'Movilidades', data: { section: 'Compras', breadcrumb: 'Movilidades' } },
];
