import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, HostListener, OnDestroy, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink, Routes } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { LookupService } from '../../../shared/services/lookup.service';
import { guardarBlob } from '../../../shared/services/report.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { RrhhApiService } from '../../rrhh/data-access/rrhh.api';
import { ServiciosApiService } from '../data-access/servicios.api';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-servicios-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PageHeaderComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, PaginationComponent, IconComponent, PenPipe],
  templateUrl: './servicios-page.component.html',
  styleUrls: ['./servicios-page.component.css'],
})
export class ServiciosPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.ServicioResponseDTO>();
  readonly filtros = this.fb.nonNullable.group({ searchTerm: [''], activo: [''] });
  readonly form = this.fb.nonNullable.group({
    id: [''],
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: ['', Validators.maxLength(5000)],
    precio: [0, [Validators.required, Validators.min(0)]],
    activo: [true],
    colaboradores: this.fb.array<FormGroup>([]),
  });
  usuarios: Api.UserResponseDTO[] = [];
  asistencias: Api.RrhhAsistenciaResponseDTO[] = [];
  disponibles: Api.RrhhAsistenciaResponseDTO[] = [];
  asistenciaError: string | null = null;
  asistenciaCargando = false;
  detalle: Api.ServicioResponseDTO | null = null;
  modal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.ServicioResponseDTO | null = null;
  private readonly buscar$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly api: ServiciosApiService,
    private readonly rrhh: RrhhApiService,
    private readonly lookup: LookupService,
    private readonly toast: ToastService,
    private readonly http: HttpClient,
  ) {
    this.state.sort = 'nombre,asc';
  }

  ngOnInit(): void {
    this.cargar();
    this.cargarAsistencia();
    this.lookup.loadUsuarios().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.usuarios = rows; });
    this.buscar$.pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe((valor) => {
      this.filtros.controls.searchTerm.setValue(valor);
      this.filtrar();
    });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  get equipo() { return this.form.controls.colaboradores; }

  cargar(): void {
    this.api.GetApiV1Servicios(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => {
        this.state.apply(page);
        if (this.detalle?.id && !this.state.rows.some((fila) => fila.id === this.detalle?.id)) this.detalle = this.state.rows[0] ?? null;
      },
      error: (error: unknown) => {
        const mensaje = problemText(error);
        this.state.fail(mensaje, problemStatus(error));
        this.toast.error(mensaje);
      },
    });
  }

  cargarAsistencia(): void {
    const hoy = this.hoy();
    this.asistenciaCargando = true;
    this.asistenciaError = null;
    this.rrhh.GetApiV1RrhhAsistenciasListar({ fechaInicio: hoy, fechaFin: hoy, page: 0, size: 200, sort: 'fechaHora,desc' })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          this.asistenciaCargando = false;
          this.asistencias = page.content ?? [];
          this.disponibles = this.disponiblesDe(this.asistencias);
        },
        error: (error: unknown) => {
          this.asistenciaCargando = false;
          this.asistencias = [];
          this.disponibles = [];
          this.asistenciaError = problemText(error);
          this.toast.error(this.asistenciaError);
        },
      });
  }

  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ searchTerm: '', activo: '' }); this.filtrar(); }
  onBuscar(event: Event): void { this.buscar$.next((event.target as HTMLInputElement).value); }
  elegirEstado(valor: '' | 'true' | 'false'): void {
    this.filtros.controls.activo.setValue(this.filtros.controls.activo.value === valor ? '' : valor);
    this.filtrar();
  }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }

  abrirCrear(): void {
    this.editando = false;
    this.errorFormulario = null;
    this.equipo.clear();
    this.form.reset({ id: '', nombre: '', descripcion: '', precio: 0, activo: true });
    this.modal = true;
  }

  abrirEditar(item: Api.ServicioResponseDTO): void {
    this.editando = true;
    this.errorFormulario = null;
    this.equipo.clear();
    this.form.reset({
      id: item.id ?? '',
      nombre: item.nombre ?? '',
      descripcion: item.descripcion ?? '',
      precio: item.precio ?? 0,
      activo: item.activo ?? true,
    });
    for (const persona of item.colaboradores ?? []) {
      if (persona.userId) this.agregarPersona(persona.userId, persona.participacion === 'INSTALO' ? 'INSTALO' : 'AYUDO');
    }
    this.detalle = item;
    this.modal = true;
  }

  agregarPersona(userId = '', participacion: 'INSTALO' | 'AYUDO' = 'AYUDO'): void {
    this.equipo.push(this.fb.nonNullable.group({
      userId: [userId, Validators.required],
      participacion: [participacion, Validators.required],
    }));
  }

  quitarPersona(indice: number): void { this.equipo.removeAt(indice); }

  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const personas = value.colaboradores as { userId: string; participacion: string }[];
    const ids = personas.map((persona) => persona.userId);
    if (new Set(ids).size !== ids.length) {
      this.errorFormulario = 'La misma persona está repetida en el equipo.';
      return;
    }
    const body: Api.ServicioRequestDTO = {
      nombre: value.nombre.trim(),
      descripcion: value.descripcion.trim() || undefined,
      precio: Number(value.precio),
      activo: value.activo,
      colaboradores: personas.map((persona) => ({
        userId: persona.userId,
        participacion: persona.participacion === 'INSTALO' ? 'INSTALO' : 'AYUDO',
      })),
    };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1ServiciosId({ id: value.id }, body) : this.api.PostApiV1Servicios({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.guardando = false;
        this.modal = false;
        this.lookup.invalidate('servicios');
        this.toast.success(this.editando ? 'Servicio actualizado.' : 'Servicio creado.');
        this.cargar();
      },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }

  pedirEliminar(row: Api.ServicioResponseDTO): void { this.seleccionado = row; this.confirmar = true; }

  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1ServiciosId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.eliminando = false;
        this.confirmar = false;
        this.lookup.invalidate('servicios');
        this.toast.success('Servicio desactivado.');
        this.cargar();
      },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }

  ver(item: Api.ServicioResponseDTO): void { this.detalle = item; }

  equipoTexto(item: Api.ServicioResponseDTO): string {
    const personas = item.colaboradores ?? [];
    if (!personas.length) return 'Sin personas asignadas';
    return personas.map((persona) => `${this.nombrePersona(persona)} · ${persona.participacion === 'INSTALO' ? 'Instaló' : 'Ayudó'}`).join(', ');
  }

  nombrePersona(persona: { nombre?: string; apellido?: string; email?: string }): string {
    return [persona.nombre, persona.apellido].filter(Boolean).join(' ') || persona.email || 'Usuario';
  }

  nombreUsuario(usuario: Api.UserResponseDTO): string {
    return [usuario.nombre, usuario.apellido].filter(Boolean).join(' ') || usuario.email || usuario.nombreUsuario || 'Usuario';
  }

  iniciales(nombre: string | undefined): string {
    const partes = (nombre ?? 'S').trim().split(/\s+/).slice(0, 2);
    return partes.map((parte) => parte.charAt(0).toUpperCase()).join('') || 'S';
  }

  errorDe(nombre: 'nombre' | 'descripcion' | 'precio'): string | null { return fieldError(this.form.controls[nombre]); }

  plantillaServicios(): void {
    this.http.get(`${environment.apiUrl}/api/v1/servicios/importar/plantilla`, { responseType: 'blob' }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (blob) => guardarBlob(blob, 'plantilla-servicios.xlsx'),
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  importarServicios(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo) return;
    const cuerpo = new FormData();
    cuerpo.append('archivo', archivo);
    this.http.post<{ creados: number; actualizados: number }>(`${environment.apiUrl}/api/v1/servicios/importar`, cuerpo).pipe(takeUntil(this.destroy$)).subscribe({
      next: (resultado) => {
        this.toast.success(`Servicios importados: ${resultado.creados} nuevos, ${resultado.actualizados} actualizados.`);
        this.cargar();
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  vacio(): string {
    return this.filtros.controls.searchTerm.value ? 'No hay resultados' : 'No hay servicios con estos filtros.';
  }

  @HostListener('document:keydown', ['$event'])
  atajo(event: KeyboardEvent): void {
    if (event.key !== 'F2' || this.modal) return;
    event.preventDefault();
    document.getElementById('srv-buscar')?.focus();
  }

  private hoy(): string {
    const fecha = new Date();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }

  private disponiblesDe(filas: Api.RrhhAsistenciaResponseDTO[]): Api.RrhhAsistenciaResponseDTO[] {
    const ultima = new Map<string, Api.RrhhAsistenciaResponseDTO>();
    const ordenadas = [...filas].sort((a, b) => (a.fechaHora ?? '').localeCompare(b.fechaHora ?? ''));
    for (const fila of ordenadas) {
      if (fila.empleadoId) ultima.set(fila.empleadoId, fila);
    }
    return [...ultima.values()].filter((fila) => fila.tipoMarcacion === 'ENTRADA' || fila.tipoMarcacion === 'FIN_RECESO');
  }
}

export const SERVICIOS_ROUTES: Routes = [
  { path: '', component: ServiciosPageComponent, title: 'Servicios', data: { section: 'Inventario', breadcrumb: 'Servicios' } },
];
