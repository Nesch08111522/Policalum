import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { ApiProblem } from '../../../core/api/openapi.types';
import { SedesApiService } from '../data-access/sedes.api';
import * as Api from '../../../core/api/openapi.types';

interface Filtros {
  activa: string;
}

interface SedeForm {
  id?: string;
  nombre: string;
  direccion: string;
  ciudad: string;
  pais: string;
  moneda: string;
  celularSede: string;
  activa: boolean;
}

@Component({
  selector: 'app-sedes-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sedes-page.component.html',
  styleUrls: ['./sedes-page.component.css'],
})
export class SedesPageComponent implements OnInit, OnDestroy {
  sedes: Api.SedeResponseDTO[] = [];
  totalElementos = 0;
  totalPaginas = 0;
  page = 0;
  size = 20;
  sort = 'nombre,asc';
  loading = false;
  error: ApiProblem | null = null;
  exito: string | null = null;

  filtros: Filtros = {
    activa: '',
  };

  private readonly destroy$ = new Subject<void>();

  mostrarFormulario = false;
  modoEdicion = false;
  guardando = false;
  errorFormulario: string | null = null;
  form: SedeForm = this.formVacio();

  mostrarConfirmacion = false;
  sedeAEliminar: Api.SedeResponseDTO | null = null;
  eliminando = false;

  constructor(private readonly sedesApi: SedesApiService) {}

  ngOnInit(): void {
    this.cargarSedes();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargarSedes(): void {
    this.loading = true;
    this.error = null;
    const params: Record<string, string | number> = {
      page: this.page,
      size: this.size,
      sort: this.sort,
    };

    // Si hay filtro por activa, usamos el endpoint específico;
    // si no, usamos el listado completo.
    const peticion$ = this.filtros.activa !== ''
      ? this.sedesApi.GetApiV1SedesEstado({ ...params, activa: this.filtros.activa })
      : this.sedesApi.GetApiV1Sedes(params);

    peticion$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (respuesta) => {
        this.sedes = respuesta.content ?? [];
        this.totalElementos = respuesta.totalElements ?? 0;
        this.totalPaginas = respuesta.totalPages ?? 0;
        this.page = respuesta.number ?? 0;
        this.loading = false;
      },
      error: (err: ApiProblem) => {
        this.error = err;
        this.sedes = [];
        this.totalElementos = 0;
        this.totalPaginas = 0;
        this.loading = false;
      },
    });
  }

  aplicarFiltros(): void {
    this.page = 0;
    this.cargarSedes();
  }

  limpiarFiltros(): void {
    this.filtros = { activa: '' };
    this.page = 0;
    this.cargarSedes();
  }

  cambiarPage(nuevaPage: number): void {
    if (nuevaPage < 0 || nuevaPage >= this.totalPaginas) return;
    this.page = nuevaPage;
    this.cargarSedes();
  }

  cambiarSize(nuevoSize: number): void {
    this.size = Number(nuevoSize);
    this.page = 0;
    this.cargarSedes();
  }

  abrirFormularioNuevo(): void {
    this.form = this.formVacio();
    this.modoEdicion = false;
    this.errorFormulario = null;
    this.mostrarFormulario = true;
  }

  abrirFormularioEditar(sede: Api.SedeResponseDTO): void {
    this.form = {
      id: sede.id,
      nombre: sede.nombre ?? '',
      direccion: sede.direccion ?? '',
      ciudad: sede.ciudad ?? '',
      pais: sede.pais ?? 'Perú',
      moneda: sede.moneda ?? 'PEN',
      celularSede: sede.celularSede ?? '',
      activa: sede.activa ?? true,
    };
    this.modoEdicion = true;
    this.errorFormulario = null;
    this.mostrarFormulario = true;
  }

  cerrarFormulario(): void {
    if (this.guardando) return;
    this.mostrarFormulario = false;
  }

  guardarSede(): void {
    this.errorFormulario = null;

    if (!this.form.nombre.trim()) {
      this.errorFormulario = 'El nombre es obligatorio.';
      return;
    }

    const body: Api.SedeRequestDTO = {
      nombre: this.form.nombre.trim(),
      direccion: this.form.direccion.trim() || undefined,
      ciudad: this.form.ciudad.trim() || undefined,
      pais: this.form.pais.trim() || 'Perú',
      moneda: this.form.moneda.trim() || 'PEN',
      celularSede: this.form.celularSede.trim() || undefined,
      activa: this.form.activa,
    };

    this.guardando = true;
    if (this.modoEdicion && this.form.id) {
      this.sedesApi.PutApiV1SedesId({ id: this.form.id }, body).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarFormulario = false;
          this.mostrarExito('Sede actualizada correctamente.');
          this.cargarSedes();
        },
        error: (err: ApiProblem) => {
          this.guardando = false;
          this.errorFormulario = this.mensajeError(err);
        },
      });
    } else {
      this.sedesApi.PostApiV1Sedes({}, body).subscribe({
        next: () => {
          this.guardando = false;
          this.mostrarFormulario = false;
          this.mostrarExito('Sede creada correctamente.');
          this.cargarSedes();
        },
        error: (err: ApiProblem) => {
          this.guardando = false;
          this.errorFormulario = this.mensajeError(err);
        },
      });
    }
  }

  confirmarEliminar(sede: Api.SedeResponseDTO): void {
    this.sedeAEliminar = sede;
    this.mostrarConfirmacion = true;
  }

  cerrarConfirmacion(): void {
    if (this.eliminando) return;
    this.mostrarConfirmacion = false;
    this.sedeAEliminar = null;
  }

  eliminarSede(): void {
    if (!this.sedeAEliminar?.id) return;
    this.eliminando = true;
    this.sedesApi.DeleteApiV1SedesId({ id: this.sedeAEliminar.id }).subscribe({
      next: () => {
        this.eliminando = false;
        this.mostrarConfirmacion = false;
        this.sedeAEliminar = null;
        this.mostrarExito('Sede eliminada correctamente.');
        this.cargarSedes();
      },
      error: (err: ApiProblem) => {
        this.eliminando = false;
        this.mostrarConfirmacion = false;
        this.error = err;
      },
    });
  }

  private formVacio(): SedeForm {
    return {
      nombre: '',
      direccion: '',
      ciudad: '',
      pais: 'Perú',
      moneda: 'PEN',
      celularSede: '',
      activa: true,
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