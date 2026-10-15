import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { ApiProblem } from '../../../core/api/openapi.types';
import { CatalogoFraccionesApiService } from '../data-access/catalogo-fracciones.api';
import { ProductosApiService } from '../../productos/data-access/productos.api';
import * as Api from '../../../core/api/openapi.types';

interface Filtros {
  productoId: string;
  searchTerm: string;
  activo: string;
}

interface FraccionForm {
  id?: string;
  productoId: string;
  nombreFraccion: string;
  factorDescuentoKardex: number | null;
  precioVenta: number | null;
  activo: boolean;
}

@Component({
  selector: 'app-catalogo-fracciones-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalogo-fracciones-page.component.html',
  styleUrls: ['./catalogo-fracciones-page.component.css'],
})
export class CatalogoFraccionesPageComponent implements OnInit, OnDestroy {
  fracciones: Api.CatalogoFraccionResponseDTO[] = [];
  totalElementos = 0;
  totalPaginas = 0;
  page = 0;
  size = 20;
  sort = 'nombreFraccion,asc';
  loading = false;
  error: ApiProblem | null = null;
  exito: string | null = null;

  filtros: Filtros = {
    productoId: '',
    searchTerm: '',
    activo: '',
  };

  // Productos fraccionables para los selects
  productos: Api.ProductoResponseDTO[] = [];

  private readonly searchSubject = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  mostrarFormulario = false;
  modoEdicion = false;
  guardando = false;
  errorFormulario: string | null = null;
  form: FraccionForm = this.formVacio();

  mostrarConfirmacion = false;
  fraccionAEliminar: Api.CatalogoFraccionResponseDTO | null = null;
  eliminando = false;

  constructor(
    private readonly catalogoFraccionesApi: CatalogoFraccionesApiService,
    private readonly productosApi: ProductosApiService,
  ) {}

  ngOnInit(): void {
    this.cargarFracciones();
    this.cargarProductos();

    this.searchSubject
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((termino) => {
        this.filtros.searchTerm = termino;
        this.page = 0;
        this.cargarFracciones();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargarFracciones(): void {
    this.loading = true;
    this.error = null;
    const params = this.construirParams();
    this.catalogoFraccionesApi.GetApiV1CatalogoFracciones(params).subscribe({
      next: (respuesta) => {
        this.fracciones = respuesta.content ?? [];
        this.totalElementos = respuesta.totalElements ?? 0;
        this.totalPaginas = respuesta.totalPages ?? 0;
        this.page = respuesta.number ?? 0;
        this.loading = false;
      },
      error: (err: ApiProblem) => {
        this.error = err;
        this.fracciones = [];
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
    if (this.filtros.productoId) params['productoId'] = this.filtros.productoId;
    if (this.filtros.searchTerm.trim()) params['searchTerm'] = this.filtros.searchTerm.trim();
    if (this.filtros.activo !== '') params['activo'] = this.filtros.activo;
    return params;
  }

  private cargarProductos(): void {
    // Solo traemos productos fraccionables, porque solo ellos pueden tener fracciones.
    this.productosApi.GetApiV1Productos({ page: 0, size: 500, esFraccionable: true }).subscribe({
      next: (respuesta) => (this.productos = respuesta.content ?? []),
      error: () => (this.productos = []),
    });
  }

  onBuscarInput(valor: string): void {
    this.searchSubject.next(valor);
  }

  aplicarFiltros(): void {
    this.page = 0;
    this.cargarFracciones();
  }

  limpiarFiltros(): void {
    this.filtros = {
      productoId: '',
      searchTerm: '',
      activo: '',
    };
    this.page = 0;
    this.cargarFracciones();
  }

  cambiarPage(nuevaPage: number): void {
    if (nuevaPage < 0 || nuevaPage >= this.totalPaginas) return;
    this.page = nuevaPage;
    this.cargarFracciones();
  }

  cambiarSize(nuevoSize: number): void {
    this.size = Number(nuevoSize);
    this.page = 0;
    this.cargarFracciones();
  }

  abrirFormularioNuevo(): void {
    this.form = this.formVacio();
    // Preselecciona el producto que esté filtrado, si hay uno
    if (this.filtros.productoId) {
      this.form.productoId = this.filtros.productoId;
    }
    this.modoEdicion = false;
    this.errorFormulario = null;
    this.mostrarFormulario = true;
  }

  abrirFormularioEditar(fraccion: Api.CatalogoFraccionResponseDTO): void {
    this.form = {
      id: fraccion.id,
      // Como el DTO de respuesta no trae productoId, usamos el filtro actual como fallback.
      // Si no hay filtro, el usuario tendrá que volver a elegirlo.
      productoId: this.filtros.productoId || '',
      nombreFraccion: fraccion.nombreFraccion ?? '',
      factorDescuentoKardex: fraccion.factorDescuentoKardex ?? null,
      precioVenta: fraccion.precioVenta ?? null,
      activo: fraccion.activo ?? true,
    };
    this.modoEdicion = true;
    this.errorFormulario = null;
    this.mostrarFormulario = true;
  }

  cerrarFormulario(): void {
    if (this.guardando) return;
    this.mostrarFormulario = false;
  }

  guardarFraccion(): void {
    this.errorFormulario = null;

    if (!this.form.productoId) {
      this.errorFormulario = 'Debes seleccionar un producto.';
      return;
    }
    if (!this.form.nombreFraccion.trim()) {
      this.errorFormulario = 'El nombre de la fracción es obligatorio.';
      return;
    }
    if (this.form.factorDescuentoKardex === null || this.form.factorDescuentoKardex <= 0 || this.form.factorDescuentoKardex > 1) {
      this.errorFormulario = 'El factor debe ser mayor que 0 y menor o igual que 1.';
      return;
    }

    const body: Api.CatalogoFraccionRequestDTO = {
      productoId: this.form.productoId,
      nombreFraccion: this.form.nombreFraccion.trim(),
      factorDescuentoKardex: this.form.factorDescuentoKardex,
      precioVenta: this.form.precioVenta ?? undefined,
      activo: this.form.activo,
    };

    this.guardando = true;
    if (this.modoEdicion && this.form.id) {
      this.catalogoFraccionesApi.PutApiV1CatalogoFraccionesId({ id: this.form.id }, body).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarFormulario = false;
          this.mostrarExito('Fracción actualizada correctamente.');
          this.cargarFracciones();
        },
        error: (err: ApiProblem) => {
          this.guardando = false;
          this.errorFormulario = this.mensajeError(err);
        },
      });
    } else {
      this.catalogoFraccionesApi.PostApiV1CatalogoFracciones({}, body).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarFormulario = false;
          this.mostrarExito('Fracción creada correctamente.');
          this.cargarFracciones();
        },
        error: (err: ApiProblem) => {
          this.guardando = false;
          this.errorFormulario = this.mensajeError(err);
        },
      });
    }
  }

  confirmarEliminar(fraccion: Api.CatalogoFraccionResponseDTO): void {
    this.fraccionAEliminar = fraccion;
    this.mostrarConfirmacion = true;
  }

  cerrarConfirmacion(): void {
    if (this.eliminando) return;
    this.mostrarConfirmacion = false;
    this.fraccionAEliminar = null;
  }

  eliminarFraccion(): void {
    if (!this.fraccionAEliminar?.id) return;
    this.eliminando = true;
    this.catalogoFraccionesApi.DeleteApiV1CatalogoFraccionesId({ id: this.fraccionAEliminar.id }).subscribe({
      next: () => {
        this.eliminando = false;
        this.mostrarConfirmacion = false;
        this.fraccionAEliminar = null;
        this.mostrarExito('Fracción eliminada correctamente.');
        this.cargarFracciones();
      },
      error: (err: ApiProblem) => {
        this.eliminando = false;
        this.mostrarConfirmacion = false;
        this.error = err;
      },
    });
  }

  private formVacio(): FraccionForm {
    return {
      productoId: '',
      nombreFraccion: '',
      factorDescuentoKardex: null,
      precioVenta: null,
      activo: true,
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

  trackById(_index: number, item: { id?: string }): string {
    return item.id ?? String(_index);
  }
}