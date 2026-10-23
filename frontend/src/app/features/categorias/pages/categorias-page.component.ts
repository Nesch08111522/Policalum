import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { ColumnDef, SelectOption } from '../../../shared/models/ui.models';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { CategoriasApiService } from '../data-access/categorias.api';

@Component({
  selector: 'app-categorias-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, SelectComponent],
  templateUrl: './categorias-page.component.html',
  styleUrls: ['./categorias-page.component.css'],
})
export class CategoriasPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly lookup = inject(LookupService);
  readonly state = new PageState<Api.CategoriaResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'nombre', label: 'Categoría', sortable: true },
    { key: 'slug', label: 'Slug' },
    { key: 'parent.nombre', label: 'Padre' },
    { key: 'activa', label: 'Activa', kind: 'bool' },
  ];
  readonly filtros = this.fb.nonNullable.group({ searchTerm: [''], activa: [''] });
  readonly form = this.fb.nonNullable.group({
    id: [''],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9]+(?:-[a-z0-9]+)*$/)]],
    parentId: [''],
    activa: [true],
  });
  padres: SelectOption[] = [];
  modal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.CategoriaResponseDTO | null = null;
  private readonly buscar$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: CategoriasApiService, private readonly lookup: LookupService, private readonly toast: ToastService) { this.state.sort = 'nombre,asc'; }
  ngOnInit(): void {
    this.cargar();
    this.cargarPadres();
    this.buscar$.pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe((valor) => { this.filtros.controls.searchTerm.setValue(valor); this.filtrar(); });
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  cargar(): void {
    this.api.GetApiV1Categorias(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }
  cargarPadres(): void {
    this.api.GetApiV1CategoriasRaices().pipe(takeUntil(this.destroy$)).subscribe({
      next: (rows) => { this.padres = (rows ?? []).filter((row) => row.id).map((row) => ({ value: row.id ?? '', label: row.nombre ?? 'Categoría' })); },
      error: () => { this.padres = []; },
    });
  }
  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ searchTerm: '', activa: '' }); this.filtrar(); }
  onBuscar(event: Event): void { this.buscar$.next((event.target as HTMLInputElement).value); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }
  sugerirSlug(): void {
    if (this.form.controls.slug.dirty && this.form.controls.slug.value) return;
    const slug = this.form.controls.nombre.value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9\s-]/g, '').trim().replace(/\s+/g, '-').replace(/-+/g, '-');
    this.form.controls.slug.setValue(slug);
  }
  abrirCrear(): void { this.editando = false; this.errorFormulario = null; this.form.reset({ id: '', nombre: '', slug: '', parentId: '', activa: true }); this.modal = true; }
  abrirEditar(row: unknown): void {
    const item = row as Api.CategoriaResponseDTO;
    this.editando = true;
    this.errorFormulario = null;
    this.form.reset({ id: item.id ?? '', nombre: item.nombre ?? '', slug: item.slug ?? '', parentId: item.parent?.id ?? '', activa: item.activa ?? true });
    this.modal = true;
  }
  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    if (value.id && value.id === value.parentId) { this.errorFormulario = 'Una categoría no puede ser su propio padre.'; return; }
    const body: Api.CategoriaRequestDTO = { nombre: value.nombre.trim(), slug: value.slug.trim(), activa: value.activa, parent: value.parentId ? { id: value.parentId } : undefined };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1CategoriasId({ id: value.id }, body) : this.api.PostApiV1Categorias({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.lookup.invalidate('categorias'); this.toast.success(this.editando ? 'Categoría actualizada.' : 'Categoría creada.'); this.cargar(); this.cargarPadres(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }
  pedirEliminar(row: unknown): void { this.seleccionado = row as Api.CategoriaResponseDTO; this.confirmar = true; }
  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1CategoriasId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.lookup.invalidate('categorias'); this.toast.success('Categoría eliminada.'); this.cargar(); this.cargarPadres(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }
  errorDe(nombre: 'nombre' | 'slug'): string | null { return fieldError(this.form.controls[nombre], { pattern: 'Usa minúsculas, números y guiones.' }); }
}

export const CATEGORIAS_ROUTES: Routes = [
  { path: '', component: CategoriasPageComponent, title: 'Categorías', data: { section: 'Inventario', breadcrumb: 'Categorías' } },
];
