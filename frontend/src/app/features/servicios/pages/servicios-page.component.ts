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
import { ColumnDef } from '../../../shared/models/ui.models';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { PageState } from '../../../shared/utils/page-state';
import { ServiciosApiService } from '../data-access/servicios.api';

@Component({
  selector: 'app-servicios-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent],
  templateUrl: './servicios-page.component.html',
  styleUrls: ['./servicios-page.component.css'],
})
export class ServiciosPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.ServicioResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'nombre', label: 'Servicio', sortable: true },
    { key: 'descripcion', label: 'Descripción' },
    { key: 'precio', label: 'Precio', kind: 'money', align: 'end', sortable: true },
    { key: 'activo', label: 'Activo', kind: 'bool' },
  ];
  readonly filtros = this.fb.nonNullable.group({ searchTerm: [''], activo: [''] });
  readonly form = this.fb.nonNullable.group({
    id: [''],
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: ['', Validators.maxLength(5000)],
    precio: [0, [Validators.required, Validators.min(0)]],
    activo: [true],
  });
  modal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.ServicioResponseDTO | null = null;
  private readonly buscar$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: ServiciosApiService, private readonly toast: ToastService) {
    this.state.sort = 'nombre,asc';
  }

  ngOnInit(): void {
    this.cargar();
    this.buscar$.pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe((valor) => {
      this.filtros.controls.searchTerm.setValue(valor);
      this.filtrar();
    });
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  cargar(): void {
    this.state.loading = true;
    this.state.error = null;
    this.api.GetApiV1Servicios(this.state.query(this.filtros.getRawValue())).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }
  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ searchTerm: '', activo: '' }); this.filtrar(); }
  onBuscar(event: Event): void { this.buscar$.next((event.target as HTMLInputElement).value); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }

  abrirCrear(): void {
    this.editando = false;
    this.errorFormulario = null;
    this.form.reset({ id: '', nombre: '', descripcion: '', precio: 0, activo: true });
    this.modal = true;
  }
  abrirEditar(row: unknown): void {
    const item = row as Api.ServicioResponseDTO;
    this.editando = true;
    this.errorFormulario = null;
    this.form.reset({ id: item.id ?? '', nombre: item.nombre ?? '', descripcion: item.descripcion ?? '', precio: item.precio ?? 0, activo: item.activo ?? true });
    this.modal = true;
  }
  guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.ServicioRequestDTO = { nombre: value.nombre.trim(), descripcion: value.descripcion.trim() || undefined, precio: Number(value.precio), activo: value.activo };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1ServiciosId({ id: value.id }, body) : this.api.PostApiV1Servicios({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.toast.success(this.editando ? 'Servicio actualizado.' : 'Servicio creado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }
  pedirEliminar(row: unknown): void { this.seleccionado = row as Api.ServicioResponseDTO; this.confirmar = true; }
  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1ServiciosId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.toast.success('Servicio eliminado.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }
  errorDe(nombre: 'nombre' | 'descripcion' | 'precio'): string | null { return fieldError(this.form.controls[nombre]); }
}

export const SERVICIOS_ROUTES: Routes = [
  { path: '', component: ServiciosPageComponent, title: 'Servicios', data: { section: 'Inventario', breadcrumb: 'Servicios' } },
];
