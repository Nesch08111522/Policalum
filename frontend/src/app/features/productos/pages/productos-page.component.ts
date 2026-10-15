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
import { ProductosApiService } from '../data-access/productos.api';

@Component({
  selector: 'app-productos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, SelectComponent],
  templateUrl: './productos-page.component.html',
  styleUrls: ['./productos-page.component.css'],
})
export class ProductosPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.ProductoResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'nombre', label: 'Producto', sortable: true },
    { key: 'codigoSku', label: 'SKU' },
    { key: 'categoria.nombre', label: 'Categoría' },
    { key: 'proveedor.razonSocial', label: 'Proveedor' },
    { key: 'tipo', label: 'Tipo', kind: 'badge', badge: { FISICO: 'info', SERVICIO: 'neutral' } },
    { key: 'precioVentaBase', label: 'Precio', kind: 'money', align: 'end' },
    { key: 'activo', label: 'Activo', kind: 'bool' },
  ];
  readonly filtros = this.fb.nonNullable.group({ searchTerm: [''], categoriaId: [''], tipo: [''], activo: [''], esFraccionable: [''] });
  readonly form = this.fb.nonNullable.group({
    id: [''],
    nombre: ['', [Validators.required, Validators.maxLength(200)]],
    codigoSku: [''],
    descripcion: [''],
    categoriaId: [''],
    proveedorId: [''],
    tipo: ['FISICO' as Api.ProductoRequestDTO['tipo'], Validators.required],
    unidad: ['UNIDAD'],
    precioCompra: [0, [Validators.required, Validators.min(0)]],
    precioVentaBase: [0, [Validators.required, Validators.min(0)]],
    activo: [true],
    esFraccionable: [false],
    incluyeIgv: [true],
  });
  categorias: SelectOption[] = [];
  proveedores: SelectOption[] = [];
  modal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.ProductoResponseDTO | null = null;
  private readonly buscar$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: ProductosApiService, private readonly lookup: LookupService, private readonly toast: ToastService) { this.state.sort = 'nombre,asc'; }
  ngOnInit(): void {
    this.cargar();
    this.cargarCombos();
    this.buscar$.pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe((valor) => { this.filtros.controls.searchTerm.setValue(valor); this.filtrar(); });
  }
  cargarCombos(): void {
    this.lookup.loadCategorias().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.categorias = this.lookup.options(rows, (row) => (row as Api.CategoriaResponseDTO).nombre ?? 'Categoría'); });
    this.lookup.loadProveedores().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.proveedores = this.lookup.options(rows, (row) => (row as Api.ProveedorResponseDTO).nombreRazon ?? 'Proveedor'); });
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  cargar(): void {
    this.api.GetApiV1Productos(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }
  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ searchTerm: '', categoriaId: '', tipo: '', activo: '', esFraccionable: '' }); this.filtrar(); }
  onBuscar(event: Event): void { this.buscar$.next((event.target as HTMLInputElement).value); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }
  abrirCrear(): void { this.cargarCombos(); this.editando = false; this.errorFormulario = null; this.form.reset({ id: '', nombre: '', codigoSku: '', descripcion: '', categoriaId: '', proveedorId: '', tipo: 'FISICO', unidad: 'UNIDAD', precioCompra: 0, precioVentaBase: 0, activo: true, esFraccionable: false, incluyeIgv: true }); this.modal = true; }
  abrirEditar(row: unknown): void {
    this.cargarCombos();
    const item = row as Api.ProductoResponseDTO;
    this.editando = true;
    this.errorFormulario = null;
    this.form.reset({
      id: item.id ?? '', nombre: item.nombre ?? '', codigoSku: item.codigoSku ?? '', descripcion: item.descripcion ?? '',
      categoriaId: item.categoria?.id ?? '', proveedorId: item.proveedor?.id ?? '', tipo: item.tipo ?? 'FISICO', unidad: item.unidad ?? 'UNIDAD',
      precioCompra: item.precioCompra ?? 0, precioVentaBase: item.precioVentaBase ?? 0, activo: item.activo ?? true, esFraccionable: item.esFraccionable ?? false, incluyeIgv: item.incluyeIgv ?? true,
    });
    this.modal = true;
  }
  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.ProductoRequestDTO = {
      nombre: value.nombre.trim(), codigoSku: value.codigoSku.trim() || undefined, descripcion: value.descripcion.trim() || undefined,
      categoriaId: value.categoriaId || undefined, proveedorId: value.proveedorId || undefined, tipo: value.tipo, unidad: value.unidad.trim() || undefined,
      precioCompra: Number(value.precioCompra), precioVentaBase: Number(value.precioVentaBase), activo: value.activo, esFraccionable: value.esFraccionable, incluyeIgv: value.incluyeIgv,
    };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1ProductosId({ id: value.id }, body) : this.api.PostApiV1Productos({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.lookup.invalidate('productos'); this.toast.success(this.editando ? 'Producto actualizado.' : 'Producto creado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }
  pedirEliminar(row: unknown): void { this.seleccionado = row as Api.ProductoResponseDTO; this.confirmar = true; }
  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1ProductosId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.lookup.invalidate('productos'); this.toast.success('Producto eliminado.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }
  errorDe(nombre: 'nombre' | 'precioCompra' | 'precioVentaBase'): string | null { return fieldError(this.form.controls[nombre]); }
}

export const PRODUCTOS_ROUTES: Routes = [
  { path: '', component: ProductosPageComponent, title: 'Productos', data: { section: 'Inventario', breadcrumb: 'Productos' } },
];
