import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { ApiProblem } from '../../../core/api/openapi.types';
import { ProductosApiService } from '../data-access/productos.api';
import { CategoriasApiService } from '../../categorias/data-access/categorias.api';
import { ProveedoresApiService } from '../../proveedores/data-access/proveedores.api';
import * as Api from '../../../core/api/openapi.types';

interface Filtros {
  searchTerm: string;
  categoriaId: string;
  tipo: string;
  activo: string;
  esFraccionable: string;
}

interface ProductoForm {
  id?: string;
  nombre: string;
  codigoSku: string;
  descripcion: string;
  categoriaId: string;
  proveedorId: string;
  tipo: 'FISICO' | 'SERVICIO';
  unidad: string;
  precioCompra: number | null;
  precioVentaBase: number | null;
  activo: boolean;
  esFraccionable: boolean;
  incluyeIgv: boolean;
}

@Component({
  selector: 'app-productos-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './productos-page.component.html',
  styleUrls: ['./productos-page.component.css'],
})
export class ProductosPageComponent implements OnInit, OnDestroy {
  productos: Api.ProductoResponseDTO[] = [];
  totalElementos = 0;
  totalPaginas = 0;
  page = 0;
  size = 20;
  sort = 'nombre,asc';
  loading = false;
  error: ApiProblem | null = null;
  exito: string | null = null;

  filtros: Filtros = {
    searchTerm: '',
    categoriaId: '',
    tipo: '',
    activo: '',
    esFraccionable: '',
  };
  categorias: Api.CategoriaResponseDTO[] = [];
  proveedores: Api.ProveedorResponseDTO[] = [];

  private readonly searchSubject = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  mostrarFormulario = false;
  modoEdicion = false;
  guardando = false;
  errorFormulario: string | null = null;
  form: ProductoForm = this.formVacio();

  mostrarConfirmacion = false;
  productoAEliminar: Api.ProductoResponseDTO | null = null;
  eliminando = false;

  constructor(
    private readonly productosApi: ProductosApiService,
    private readonly categoriasApi: CategoriasApiService,
    private readonly proveedoresApi: ProveedoresApiService,
  ) {}

  ngOnInit(): void {
    this.cargarProductos();
    this.cargarCatalogos();

    this.searchSubject
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((termino) => {
        this.filtros.searchTerm = termino;
        this.page = 0;
        this.cargarProductos();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargarProductos(): void {
    this.loading = true;
    this.error = null;
    const params = this.construirParams();
    this.productosApi.GetApiV1Productos(params).subscribe({
      next: (respuesta) => {
        this.productos = respuesta.content ?? [];
        this.totalElementos = respuesta.totalElements ?? 0;
        this.totalPaginas = respuesta.totalPages ?? 0;
        this.page = respuesta.number ?? 0;
        this.loading = false;
      },
      error: (err: ApiProblem) => {
        this.error = err;
        this.productos = [];
        this.totalElementos = 0;
        this.totalPaginas = 0;
        this.loading = false;
      },
    });
  }

  private construirParams(): Record<string, string | number> {
    const params: Record<string, string | number> = {
      page: this.page,
      size: this.size,
      sort: this.sort,
    };
    if (this.filtros.searchTerm.trim()) params['searchTerm'] = this.filtros.searchTerm.trim();
    if (this.filtros.categoriaId) params['categoriaId'] = this.filtros.categoriaId;
    if (this.filtros.tipo) params['tipo'] = this.filtros.tipo;
    if (this.filtros.activo !== '') params['activo'] = this.filtros.activo;
    if (this.filtros.esFraccionable !== '') params['esFraccionable'] = this.filtros.esFraccionable;
    return params;
  }

  private cargarCatalogos(): void {
    this.categoriasApi.GetApiV1Categorias({ page: 0, size: 500 }).subscribe({
      next: (respuesta) => (this.categorias = respuesta.content ?? []),
      error: () => (this.categorias = []),
    });
    this.proveedoresApi.GetApiV1Proveedores({ page: 0, size: 500 }).subscribe({
      next: (respuesta) => (this.proveedores = respuesta.content ?? []),
      error: () => (this.proveedores = []),
    });
  }

  onBuscarInput(valor: string): void {
    this.searchSubject.next(valor);
  }

  aplicarFiltros(): void {
    this.page = 0;
    this.cargarProductos();
  }

  limpiarFiltros(): void {
    this.filtros = {
      searchTerm: '',
      categoriaId: '',
      tipo: '',
      activo: '',
      esFraccionable: '',
    };
    this.page = 0;
    this.cargarProductos();
  }

  cambiarPage(nuevaPage: number): void {
    if (nuevaPage < 0 || nuevaPage >= this.totalPaginas) return;
    this.page = nuevaPage;
    this.cargarProductos();
  }

  cambiarSize(nuevoSize: number): void {
    this.size = Number(nuevoSize);
    this.page = 0;
    this.cargarProductos();
  }

  abrirFormularioNuevo(): void {
    this.form = this.formVacio();
    this.modoEdicion = false;
    this.errorFormulario = null;
    this.mostrarFormulario = true;
  }

  abrirFormularioEditar(producto: Api.ProductoResponseDTO): void {
    this.form = {
      id: producto.id,
      nombre: producto.nombre ?? '',
      codigoSku: producto.codigoSku ?? '',
      descripcion: producto.descripcion ?? '',
      categoriaId: producto.categoria?.id ?? '',
      proveedorId: producto.proveedor?.id ?? '',
      tipo: producto.tipo ?? 'FISICO',
      unidad: producto.unidad ?? 'UNIDAD',
      precioCompra: producto.precioCompra ?? 0,
      precioVentaBase: producto.precioVentaBase ?? 0,
      activo: producto.activo ?? true,
      esFraccionable: producto.esFraccionable ?? false,
      incluyeIgv: producto.incluyeIgv ?? true,
    };
    this.modoEdicion = true;
    this.errorFormulario = null;
    this.mostrarFormulario = true;
  }

  cerrarFormulario(): void {
    if (this.guardando) return;
    this.mostrarFormulario = false;
  }

  guardarProducto(): void {
    this.errorFormulario = null;

    if (!this.form.nombre.trim()) {
      this.errorFormulario = 'El nombre es obligatorio.';
      return;
    }
    if (this.form.precioCompra === null || this.form.precioCompra < 0) {
      this.errorFormulario = 'El precio de compra debe ser mayor o igual a 0.';
      return;
    }
    if (this.form.precioVentaBase === null || this.form.precioVentaBase < 0) {
      this.errorFormulario = 'El precio de venta debe ser mayor o igual a 0.';
      return;
    }

    const body: Api.ProductoRequestDTO = {
      nombre: this.form.nombre.trim(),
      codigoSku: this.form.codigoSku.trim() || undefined,
      descripcion: this.form.descripcion.trim() || undefined,
      categoriaId: this.form.categoriaId || undefined,
      proveedorId: this.form.proveedorId || undefined,
      tipo: this.form.tipo,
      unidad: this.form.unidad.trim() || undefined,
      precioCompra: this.form.precioCompra,
      precioVentaBase: this.form.precioVentaBase,
      activo: this.form.activo,
      esFraccionable: this.form.esFraccionable,
      incluyeIgv: this.form.incluyeIgv,
    };

    this.guardando = true;
    if (this.modoEdicion && this.form.id) {
      this.productosApi.PutApiV1ProductosId({ id: this.form.id }, body).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarFormulario = false;
          this.mostrarExito('Producto actualizado correctamente.');
          this.cargarProductos();
        },
        error: (err: ApiProblem) => {
          this.guardando = false;
          this.errorFormulario = this.mensajeError(err);
        },
      });
    } else {
      this.productosApi.PostApiV1Productos({}, body).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarFormulario = false;
          this.mostrarExito('Producto creado correctamente.');
          this.cargarProductos();
        },
        error: (err: ApiProblem) => {
          this.guardando = false;
          this.errorFormulario = this.mensajeError(err);
        },
      });
    }
  }

  confirmarEliminar(producto: Api.ProductoResponseDTO): void {
    this.productoAEliminar = producto;
    this.mostrarConfirmacion = true;
  }

  cerrarConfirmacion(): void {
    if (this.eliminando) return;
    this.mostrarConfirmacion = false;
    this.productoAEliminar = null;
  }

  eliminarProducto(): void {
    if (!this.productoAEliminar?.id) return;
    this.eliminando = true;
    this.productosApi.DeleteApiV1ProductosId({ id: this.productoAEliminar.id }).subscribe({
      next: () => {
        this.eliminando = false;
        this.mostrarConfirmacion = false;
        this.productoAEliminar = null;
        this.mostrarExito('Producto eliminado correctamente.');
        this.cargarProductos();
      },
      error: (err: ApiProblem) => {
        this.eliminando = false;
        this.mostrarConfirmacion = false;
        this.error = err;
      },
    });
  }

  private formVacio(): ProductoForm {
    return {
      nombre: '',
      codigoSku: '',
      descripcion: '',
      categoriaId: '',
      proveedorId: '',
      tipo: 'FISICO',
      unidad: 'UNIDAD',
      precioCompra: 0,
      precioVentaBase: 0,
      activo: true,
      esFraccionable: false,
      incluyeIgv: true,
    };
  }

  private mostrarExito(mensaje: string): void {
    this.exito = mensaje;
    setTimeout(() => (this.exito = null), 4000);
  }

  private mensajeError(err: ApiProblem): string {
    if (err.fieldErrors && Object.keys(err.fieldErrors).length > 0) {
      return Object.entries(err.fieldErrors)
        .map(([campo, mensaje]) => `${campo}: ${mensaje}`)
        .join(' · ');
    }
    return err.message ?? 'No se pudo completar la operación.';
  }

  formatearPrecio(valor?: number | null): string {
    if (valor === null || valor === undefined) return '—';
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor);
  }

  rangoMostrado(): string {
    if (this.totalElementos === 0) return '0';
    const inicio = this.page * this.size + 1;
    const fin = Math.min((this.page + 1) * this.size, this.totalElementos);
    return `${inicio}–${fin} de ${this.totalElementos}`;
  }

  paginasVisibles(): number[] {
    const total = this.totalPaginas;
    const actual = this.page;
    const rango: number[] = [];
    const inicio = Math.max(0, actual - 2);
    const fin = Math.min(total - 1, actual + 2);
    for (let i = inicio; i <= fin; i++) rango.push(i);
    return rango;
  }

  // ✅ NUEVO: método trackBy que faltaba en el HTML
  trackById(_index: number, item: { id?: string }): string {
    return item.id ?? String(_index);
  }
}