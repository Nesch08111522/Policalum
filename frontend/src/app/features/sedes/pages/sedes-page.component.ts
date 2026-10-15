import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { RoleGuard } from '../../../guards/role.guard';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { ColumnDef } from '../../../shared/models/ui.models';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { PageState } from '../../../shared/utils/page-state';
import { SedesApiService } from '../data-access/sedes.api';

@Component({
  selector: 'app-sedes-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent],
  templateUrl: './sedes-page.component.html',
  styleUrls: ['./sedes-page.component.css'],
})
export class SedesPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.SedeResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'nombre', label: 'Sede', sortable: true },
    { key: 'ciudad', label: 'Ciudad' },
    { key: 'direccion', label: 'Dirección' },
    { key: 'celularSede', label: 'Celular' },
    { key: 'moneda', label: 'Moneda' },
    { key: 'activa', label: 'Activa', kind: 'bool' },
  ];
  readonly filtros = this.fb.nonNullable.group({ texto: [''], activa: [''] });
  readonly form = this.fb.nonNullable.group({
    id: [''],
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    pais: ['Perú', Validators.maxLength(100)],
    ciudad: ['', Validators.maxLength(100)],
    direccion: ['', Validators.maxLength(255)],
    celularSede: ['', [Validators.maxLength(20), Validators.pattern(/^[0-9+\-() ]*$/)]],
    moneda: ['PEN', Validators.maxLength(10)],
    activa: [true],
  });
  private todos: Api.SedeResponseDTO[] = [];
  modal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.SedeResponseDTO | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: SedesApiService, private readonly toast: ToastService) { this.state.sort = 'nombre,asc'; }
  ngOnInit(): void { this.cargar(); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  cargar(): void {
    this.state.loading = true;
    this.state.error = null;
    const activa = this.filtros.controls.activa.value;
    const request = activa === ''
      ? this.api.GetApiV1Sedes({ page: 0, size: 200, sort: this.state.sort })
      : this.api.GetApiV1SedesEstado({ activa, page: 0, size: 200, sort: this.state.sort });
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => { this.todos = page.content ?? []; this.state.loading = false; this.paginar(); },
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }
  paginar(): void {
    const texto = this.filtros.controls.texto.value.trim().toLowerCase();
    const filtradas = this.todos.filter((sede) => !texto || `${sede.nombre ?? ''} ${sede.ciudad ?? ''} ${sede.direccion ?? ''}`.toLowerCase().includes(texto));
    this.state.totalElements = filtradas.length;
    this.state.totalPages = filtradas.length ? Math.ceil(filtradas.length / this.state.size) : 0;
    if (this.state.totalPages > 0 && this.state.page >= this.state.totalPages) this.state.page = 0;
    this.state.rows = filtradas.slice(this.state.page * this.state.size, (this.state.page + 1) * this.state.size);
    this.state.error = null;
  }
  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ texto: '', activa: '' }); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.paginar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.paginar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }

  abrirCrear(): void { this.editando = false; this.errorFormulario = null; this.form.reset({ id: '', nombre: '', pais: 'Perú', ciudad: '', direccion: '', celularSede: '', moneda: 'PEN', activa: true }); this.modal = true; }
  abrirEditar(row: unknown): void {
    const sede = row as Api.SedeResponseDTO;
    this.editando = true;
    this.errorFormulario = null;
    this.form.reset({ id: sede.id ?? '', nombre: sede.nombre ?? '', pais: sede.pais ?? '', ciudad: sede.ciudad ?? '', direccion: sede.direccion ?? '', celularSede: sede.celularSede ?? '', moneda: sede.moneda ?? 'PEN', activa: sede.activa ?? true });
    this.modal = true;
  }
  guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.SedeRequestDTO = { nombre: value.nombre.trim(), pais: value.pais.trim() || undefined, ciudad: value.ciudad.trim() || undefined, direccion: value.direccion.trim() || undefined, celularSede: value.celularSede.trim() || undefined, moneda: value.moneda.trim() || undefined, activa: value.activa };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1SedesId({ id: value.id }, body) : this.api.PostApiV1Sedes({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.toast.success(this.editando ? 'Sede actualizada.' : 'Sede creada.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }
  pedirEliminar(row: unknown): void { this.seleccionado = row as Api.SedeResponseDTO; this.confirmar = true; }
  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1SedesId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.toast.success('Sede eliminada.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }
  errorDe(nombre: 'nombre' | 'celularSede'): string | null { return fieldError(this.form.controls[nombre], { pattern: 'Usa solo números, espacios y + - ( ).' }); }
}

export const SEDES_ROUTES: Routes = [{
  path: '',
  component: SedesPageComponent,
  canActivate: [RoleGuard],
  title: 'Sedes',
  data: { expectedRoles: ['ROLE_ADMIN'], section: 'Configuración', breadcrumb: 'Sedes' },
}];
