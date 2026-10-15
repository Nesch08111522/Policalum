import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { ApiProblem } from '../../../core/api/openapi.types';
import { AlmacenesApiService } from '../data-access/almacenes.api';
import { SedesApiService } from '../../sedes/data-access/sedes.api';
import * as Api from '../../../core/api/openapi.types';

interface Filtros {
  searchTerm: string;
  sedeId: string;
  activo: string;
}

interface AlmacenForm {
  id?: string;
  nombre: string;
  descripcion: string;
  sedeId: string;
  activo: boolean;
}

@Component({
  selector: 'app-almacenes-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './almacenes-page.component.html',
  styleUrls: ['./almacenes-page.component.css'],
})
export class AlmacenesPageComponent implements OnInit, OnDestroy {
  almacenes: Api.AlmacenResponseDTO[] = [];
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
    sedeId: '',
    activo: '',
  };
  sedes: Api.SedeResponseDTO[] = [];

  private readonly searchSubject = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  mostrarFormulario = false;
  modoEdicion = false;
  guardando = false;
  errorFormulario: string | null = null;
  form: AlmacenForm = this.formVacio();

  mostrarConfirmacion = false;
  almacenAEliminar: Api.AlmacenResponseDTO | null = null;
  eliminando = false;

  constructor(
    private readonly almacenesApi: AlmacenesApiService,
    private readonly sedesApi: SedesApiService,
  ) {}

  ngOnInit(): void {
    this.cargarAlmacenes();
    this.cargarSedes();

    this.searchSubject
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((termino) => {
        this.filtros.searchTerm = termino;
        this.page = 0;
        this.cargarAlmacenes();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargarAlmacenes(): void {
    this.loading = true;
    this.error = null;
    const params = this.construirParams();
    this.almacenesApi.GetApiV1Almacenes(params).subscribe({
      next: (respuesta) => {
        this.almacenes = respuesta.content ?? [];
        this.totalElementos = respuesta.totalElements ?? 0;
        this.totalPaginas = respuesta.totalPages ?? 0;
        this.page = respuesta.number ?? 0;
        this.loading = false;
      },
      error: (err: ApiProblem) => {
        this.error = err;
        this.almacenes = [];
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
    if (this.filtros.sedeId) params['sedeId'] = this.filtros.sedeId;
    if (this.filtros.activo !== '') params['activo'] = this.filtros.activo;
    return params;
  }

  private cargarSedes(): void {
    this.sedesApi.GetApiV1Sedes({ page: 0, size: 500 }).subscribe({
      next: (respuesta) => (this.sedes = respuesta.content ?? []),
      error: () => (this.sedes = []),
    });
  }

  onBuscarInput(valor: string): void {
    this.searchSubject.next(valor);
  }

  aplicarFiltros(): void {
    this.page = 0;
    this.cargarAlmacenes();
  }

  limpiarFiltros(): void {
    this.filtros = {
      searchTerm: '',
      sedeId: '',
      activo: '',
    };
    this.page = 0;
    this.cargarAlmacenes();
  }

  cambiarPage(nuevaPage: number): void {
    if (nuevaPage < 0 || nuevaPage >= this.totalPaginas) return;
    this.page = nuevaPage;
    this.cargarAlmacenes();
  }

  cambiarSize(nuevoSize: number): void {
    this.size = Number(nuevoSize);
    this.page = 0;
    this.cargarAlmacenes();
  }

  abrirFormularioNuevo(): void {
    this.form = this.formVacio();
    this.modoEdicion = false;
    this.errorFormulario = null;
    this.mostrarFormulario = true;
  }

  abrirFormularioEditar(almacen: Api.AlmacenResponseDTO): void {
    this.form = {
      id: almacen.id,
      nombre: almacen.nombre ?? '',
      descripcion: almacen.descripcion ?? '',
      sedeId: almacen.sede?.id ?? '',
      activo: almacen.activo ?? true,
    };
    this.modoEdicion = true;
    this.errorFormulario = null;
    this.mostrarFormulario = true;
  }

  cerrarFormulario(): void {
    if (this.guardando) return;
    this.mostrarFormulario = false;
  }

  guardarAlmacen(): void {
    this.errorFormulario = null;

    if (!this.form.nombre.trim()) {
      this.errorFormulario = 'El nombre es obligatorio.';
      return;
    }
    if (!this.form.sedeId) {
      this.errorFormulario = 'Debes seleccionar una sede.';
      return;
    }

    const sedeSeleccionada = this.sedes.find((s) => s.id === this.form.sedeId);
    if (!sedeSeleccionada) {
      this.errorFormulario = 'La sede seleccionada ya no está disponible.';
      return;
    }

    // El backend espera la entidad Sede completa, no solo el id.
    const body: Api.AlmacenRequestDTO = {
      nombre: this.form.nombre.trim(),
      descripcion: this.form.descripcion.trim() || undefined,
      activo: this.form.activo,
      sede: {
        id: sedeSeleccionada.id,
        nombre: sedeSeleccionada.nombre,
        activa: sedeSeleccionada.activa,
        ciudad: sedeSeleccionada.ciudad,
        direccion: sedeSeleccionada.direccion,
        pais: sedeSeleccionada.pais,
        moneda: sedeSeleccionada.moneda,
        celularSede: sedeSeleccionada.celularSede,
      },
    };

    this.guardando = true;
    if (this.modoEdicion && this.form.id) {
      this.almacenesApi.PutApiV1AlmacenesId({ id: this.form.id }, body).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarFormulario = false;
          this.mostrarExito('Almacén actualizado correctamente.');
          this.cargarAlmacenes();
        },
        error: (err: ApiProblem) => {
          this.guardando = false;
          this.errorFormulario = this.mensajeError(err);
        },
      });
    } else {
      this.almacenesApi.PostApiV1Almacenes({}, body).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarFormulario = false;
          this.mostrarExito('Almacén creado correctamente.');
          this.cargarAlmacenes();
        },
        error: (err: ApiProblem) => {
          this.guardando = false;
          this.errorFormulario = this.mensajeError(err);
        },
      });
    }
  }

  confirmarEliminar(almacen: Api.AlmacenResponseDTO): void {
    this.almacenAEliminar = almacen;
    this.mostrarConfirmacion = true;
  }

  cerrarConfirmacion(): void {
    if (this.eliminando) return;
    this.mostrarConfirmacion = false;
    this.almacenAEliminar = null;
  }

  eliminarAlmacen(): void {
    if (!this.almacenAEliminar?.id) return;
    this.eliminando = true;
    this.almacenesApi.DeleteApiV1AlmacenesId({ id: this.almacenAEliminar.id }).subscribe({
      next: () => {
        this.eliminando = false;
        this.mostrarConfirmacion = false;
        this.almacenAEliminar = null;
        this.mostrarExito('Almacén eliminado correctamente.');
        this.cargarAlmacenes();
      },
      error: (err: ApiProblem) => {
        this.eliminando = false;
        this.mostrarConfirmacion = false;
        this.error = err;
      },
    });
  }

  private formVacio(): AlmacenForm {
    return {
      nombre: '',
      descripcion: '',
      sedeId: '',
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