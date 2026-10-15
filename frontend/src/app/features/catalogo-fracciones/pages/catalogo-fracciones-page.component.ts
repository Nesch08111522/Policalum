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
import { ColumnDef, SelectOption } from '../../../shared/models/ui.models';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { PageState } from '../../../shared/utils/page-state';
import { CatalogoFraccionesApiService } from '../data-access/catalogo-fracciones.api';

@Component({
  selector: 'app-catalogo-fracciones-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, SelectComponent],
  templateUrl: './catalogo-fracciones-page.component.html',
  styleUrls: ['./catalogo-fracciones-page.component.css'],
})
export class CatalogoFraccionesPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.CatalogoFraccionResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'productoNombre', label: 'Producto' },
    { key: 'nombreFraccion', label: 'Fracción', sortable: true },
    { key: 'factorDescuentoKardex', label: 'Factor', kind: 'number', align: 'end' },
    { key: 'precioVenta', label: 'Precio', kind: 'money', align: 'end' },
    { key: 'activo', label: 'Activo', kind: 'bool' },
  ];
  readonly filtros = this.fb.nonNullable.group({ searchTerm: [''], productoId: [''], activo: [''] });
  readonly form = this.fb.nonNullable.group({
    id: [''],
    productoId: ['', Validators.required],
    nombreFraccion: ['', [Validators.required, Validators.maxLength(50)]],
    factorDescuentoKardex: [1, [Validators.required, Validators.min(0.0001)]],
    precioVenta: [0, Validators.min(0)],
    activo: [true],
  });
  productos: SelectOption[] = [];
  modal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.CatalogoFraccionResponseDTO | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: CatalogoFraccionesApiService, private readonly lookup: LookupService, private readonly toast: ToastService) { this.state.sort = 'nombreFraccion,asc'; }
  ngOnInit(): void {
    this.cargar();
    this.lookup.loadProductos().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.productos = this.lookup.options(rows, (row) => (row as Api.ProductoResponseDTO).nombre ?? 'Producto'); });
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  cargar(): void {
    this.state.loading = true;
    this.api.GetApiV1CatalogoFracciones(this.state.query(this.filtros.getRawValue())).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }
  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ searchTerm: '', productoId: '', activo: '' }); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  abrirCrear(): void { this.editando = false; this.errorFormulario = null; this.form.reset({ id: '', productoId: '', nombreFraccion: '', factorDescuentoKardex: 1, precioVenta: 0, activo: true }); this.modal = true; }
  abrirEditar(row: unknown): void {
    const item = row as Api.CatalogoFraccionResponseDTO;
    this.editando = true;
    this.errorFormulario = null;
    this.form.reset({ id: item.id ?? '', productoId: item.productoId ?? '', nombreFraccion: item.nombreFraccion ?? '', factorDescuentoKardex: item.factorDescuentoKardex ?? 1, precioVenta: item.precioVenta ?? 0, activo: item.activo ?? true });
    this.modal = true;
  }
  guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.CatalogoFraccionRequestDTO = { productoId: value.productoId, nombreFraccion: value.nombreFraccion.trim(), factorDescuentoKardex: Number(value.factorDescuentoKardex), precioVenta: Number(value.precioVenta), activo: value.activo };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1CatalogoFraccionesId({ id: value.id }, body) : this.api.PostApiV1CatalogoFracciones({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.toast.success(this.editando ? 'Fracción actualizada.' : 'Fracción creada.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }
  pedirEliminar(row: unknown): void { this.seleccionado = row as Api.CatalogoFraccionResponseDTO; this.confirmar = true; }
  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1CatalogoFraccionesId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.toast.success('Fracción eliminada.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }
  errorDe(nombre: 'productoId' | 'nombreFraccion' | 'factorDescuentoKardex'): string | null { return fieldError(this.form.controls[nombre]); }
}

export const CATALOGO_FRACCIONES_ROUTES: Routes = [
  { path: '', component: CatalogoFraccionesPageComponent, title: 'Fracciones', data: { section: 'Inventario', breadcrumb: 'Fracciones' } },
];
