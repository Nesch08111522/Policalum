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
import { PageState } from '../../../shared/utils/page-state';
import { AlmacenesApiService } from '../data-access/almacenes.api';

@Component({
  selector: 'app-almacenes-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, SelectComponent],
  templateUrl: './almacenes-page.component.html',
  styleUrls: ['./almacenes-page.component.css'],
})
export class AlmacenesPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.AlmacenResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'nombre', label: 'Almacén', sortable: true },
    { key: 'sede.nombre', label: 'Sede' },
    { key: 'descripcion', label: 'Descripción' },
    { key: 'activo', label: 'Activo', kind: 'bool' },
  ];
  readonly filtros = this.fb.nonNullable.group({ searchTerm: [''], sedeId: [''], activo: [''] });
  readonly form = this.fb.nonNullable.group({
    id: [''],
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: [''],
    sedeId: ['', Validators.required],
    activo: [true],
  });
  sedes: SelectOption[] = [];
  modal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.AlmacenResponseDTO | null = null;
  private readonly buscar$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: AlmacenesApiService, private readonly lookup: LookupService, private readonly toast: ToastService) { this.state.sort = 'nombre,asc'; }
  ngOnInit(): void {
    this.cargar();
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede'); });
    this.buscar$.pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe((valor) => { this.filtros.controls.searchTerm.setValue(valor); this.filtrar(); });
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  cargar(): void {
    this.state.loading = true;
    this.state.error = null;
    this.api.GetApiV1Almacenes(this.state.query(this.filtros.getRawValue())).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }
  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ searchTerm: '', sedeId: '', activo: '' }); this.filtrar(); }
  onBuscar(event: Event): void { this.buscar$.next((event.target as HTMLInputElement).value); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }
  abrirCrear(): void { this.editando = false; this.errorFormulario = null; this.form.reset({ id: '', nombre: '', descripcion: '', sedeId: '', activo: true }); this.modal = true; }
  abrirEditar(row: unknown): void {
    const item = row as Api.AlmacenResponseDTO;
    this.editando = true;
    this.errorFormulario = null;
    this.form.reset({ id: item.id ?? '', nombre: item.nombre ?? '', descripcion: item.descripcion ?? '', sedeId: item.sede?.id ?? '', activo: item.activo ?? true });
    this.modal = true;
  }
  guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.AlmacenRequestDTO = { nombre: value.nombre.trim(), descripcion: value.descripcion.trim() || undefined, activo: value.activo, sede: { id: value.sedeId } };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1AlmacenesId({ id: value.id }, body) : this.api.PostApiV1Almacenes({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.toast.success(this.editando ? 'Almacén actualizado.' : 'Almacén creado.'); this.lookup.almacenes = []; this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }
  pedirEliminar(row: unknown): void { this.seleccionado = row as Api.AlmacenResponseDTO; this.confirmar = true; }
  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1AlmacenesId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.toast.success('Almacén eliminado.'); this.lookup.almacenes = []; this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }
  errorDe(nombre: 'nombre' | 'sedeId'): string | null { return fieldError(this.form.controls[nombre]); }
}

export const ALMACENES_ROUTES: Routes = [
  { path: '', component: AlmacenesPageComponent, title: 'Almacenes', data: { section: 'Inventario', breadcrumb: 'Almacenes' } },
];
