import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { ApiProblem } from '../../../core/api/openapi.types';
import { CategoriasApiService } from '../data-access/categorias.api';
import * as Api from '../../../core/api/openapi.types';

interface Filtros {
  searchTerm: string;
  activa: string;
}

interface CategoriaForm {
  id?: string;
  nombre: string;
  slug: string;
  activa: boolean;
  parentId: string;
}

@Component({
  selector: 'app-categorias-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './categorias-page.component.html',
  styleUrls: ['./categorias-page.component.css'],
})
export class CategoriasPageComponent implements OnInit, OnDestroy {
  categorias: Api.CategoriaResponseDTO[] = [];
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
    activa: '',
  };

  // Categorías candidatas a ser "parent" en el formulario.
  // Traemos las raíces por separado para el select del modal.
  categoriasPadre: Api.CategoriaResponseDTO[] = [];

  private readonly searchSubject = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  mostrarFormulario = false;
  modoEdicion = false;
  guardando = false;
  errorFormulario: string | null = null;
  form: CategoriaForm = this.formVacio();

  mostrarConfirmacion = false;
  categoriaAEliminar: Api.CategoriaResponseDTO | null = null;
  eliminando = false;

  constructor(private readonly categoriasApi: CategoriasApiService) {}

  ngOnInit(): void {
    this.cargarCategorias();
    this.cargarCategoriasPadre();

    this.searchSubject
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((termino) => {
        this.filtros.searchTerm = termino;
        this.page = 0;
        this.cargarCategorias();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargarCategorias(): void {
    this.loading = true;
    this.error = null;
    const params = this.construirParams();
    this.categoriasApi.GetApiV1Categorias(params).subscribe({
      next: (respuesta) => {
        this.categorias = respuesta.content ?? [];
        this.totalElementos = respuesta.totalElements ?? 0;
        this.totalPaginas = respuesta.totalPages ?? 0;
        this.page = respuesta.number ?? 0;
        this.loading = false;
      },
      error: (err: ApiProblem) => {
        this.error = err;
        this.categorias = [];
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
    if (this.filtros.activa !== '') params['activa'] = this.filtros.activa;
    return params;
  }

  private cargarCategoriasPadre(): void {
    // Traemos las categorías raíz (sin parent) para usarlas como opciones del select.
    // El backend expone /api/v1/categorias/raices para esto.
    this.categoriasApi.GetApiV1CategoriasRaices().subscribe({
      next: (respuesta) => (this.categoriasPadre = respuesta ?? []),
      error: () => (this.categoriasPadre = []),
    });
  }

  onBuscarInput(valor: string): void {
    this.searchSubject.next(valor);
  }

  aplicarFiltros(): void {
    this.page = 0;
    this.cargarCategorias();
  }

  limpiarFiltros(): void {
    this.filtros = {
      searchTerm: '',
      activa: '',
    };
    this.page = 0;
    this.cargarCategorias();
  }

  cambiarPage(nuevaPage: number): void {
    if (nuevaPage < 0 || nuevaPage >= this.totalPaginas) return;
    this.page = nuevaPage;
    this.cargarCategorias();
  }

  cambiarSize(nuevoSize: number): void {
    this.size = Number(nuevoSize);
    this.page = 0;
    this.cargarCategorias();
  }

  abrirFormularioNuevo(): void {
    this.form = this.formVacio();
    this.modoEdicion = false;
    this.errorFormulario = null;
    this.mostrarFormulario = true;
  }

  abrirFormularioEditar(categoria: Api.CategoriaResponseDTO): void {
    this.form = {
      id: categoria.id,
      nombre: categoria.nombre ?? '',
      slug: categoria.slug ?? '',
      activa: categoria.activa ?? true,
      parentId: categoria.parent?.id ?? '',
    };
    this.modoEdicion = true;
    this.errorFormulario = null;
    this.mostrarFormulario = true;
  }

  cerrarFormulario(): void {
    if (this.guardando) return;
    this.mostrarFormulario = false;
  }

  guardarCategoria(): void {
    this.errorFormulario = null;

    if (!this.form.nombre.trim()) {
      this.errorFormulario = 'El nombre es obligatorio.';
      return;
    }
    if (!this.form.slug.trim()) {
      this.errorFormulario = 'El slug es obligatorio.';
      return;
    }

    // Evitamos que una categoría sea su propio parent
    if (this.modoEdicion && this.form.id === this.form.parentId) {
      this.errorFormulario = 'Una categoría no puede ser su propio padre.';
      return;
    }

    const body: Api.CategoriaRequestDTO = {
      nombre: this.form.nombre.trim(),
      slug: this.form.slug.trim().toLowerCase(),
      activa: this.form.activa,
      parent: this.form.parentId
        ? { id: this.form.parentId } as Api.Categoria
        : undefined,
    };

    this.guardando = true;
    if (this.modoEdicion && this.form.id) {
      this.categoriasApi.PutApiV1CategoriasId({ id: this.form.id }, body).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarFormulario = false;
          this.mostrarExito('Categoría actualizada correctamente.');
          this.cargarCategorias();
          this.cargarCategoriasPadre();
        },
        error: (err: ApiProblem) => {
          this.guardando = false;
          this.errorFormulario = this.mensajeError(err);
        },
      });
    } else {
      this.categoriasApi.PostApiV1Categorias({}, body).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarFormulario = false;
          this.mostrarExito('Categoría creada correctamente.');
          this.cargarCategorias();
          this.cargarCategoriasPadre();
        },
        error: (err: ApiProblem) => {
          this.guardando = false;
          this.errorFormulario = this.mensajeError(err);
        },
      });
    }
  }

  confirmarEliminar(categoria: Api.CategoriaResponseDTO): void {
    this.categoriaAEliminar = categoria;
    this.mostrarConfirmacion = true;
  }

  cerrarConfirmacion(): void {
    if (this.eliminando) return;
    this.mostrarConfirmacion = false;
    this.categoriaAEliminar = null;
  }

  eliminarCategoria(): void {
    if (!this.categoriaAEliminar?.id) return;
    this.eliminando = true;
    this.categoriasApi.DeleteApiV1CategoriasId({ id: this.categoriaAEliminar.id }).subscribe({
      next: () => {
        this.eliminando = false;
        this.mostrarConfirmacion = false;
        this.categoriaAEliminar = null;
        this.mostrarExito('Categoría eliminada correctamente.');
        this.cargarCategorias();
        this.cargarCategoriasPadre();
      },
      error: (err: ApiProblem) => {
        this.eliminando = false;
        this.mostrarConfirmacion = false;
        this.error = err;
      },
    });
  }

  // Sugiere un slug a partir del nombre
  generarSlug(): void {
    if (!this.form.nombre.trim()) return;
    this.form.slug = this.form.nombre
      .toLowerCase()
      .trim()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '') // quita acentos
      .replace(/[^a-z0-9\s-]/g, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-');
  }

  private formVacio(): CategoriaForm {
    return {
      nombre: '',
      slug: '',
      activa: true,
      parentId: '',
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