import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { RoleGuard } from '../../../guards/role.guard';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { LoaderComponent } from '../../../shared/components/loader/loader.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { RolesApiService } from '../data-access/roles.api';

interface AccionCatalogo {
  id: string;
  label: string;
}

interface ModuloCatalogo {
  id: string;
  label: string;
  acciones: AccionCatalogo[];
}

interface AccionEditor {
  id: string;
  label: string;
  activo: boolean;
  forma: 'bool' | 'mapa';
}

interface GrupoEditor {
  id: string;
  label: string;
  venia: boolean;
  acciones: AccionEditor[];
}

interface GrupoVisible {
  label: string;
  acciones: string[];
}

const MODULOS: Record<string, string> = {
  sedes: 'Sedes',
  finanzas: 'Finanzas',
  usuarios: 'Usuarios',
  asistencia: 'Asistencia',
  ventas: 'Ventas',
  caja: 'Caja',
};

const ACCIONES: Record<string, string> = {
  crear: 'Crear',
  leer: 'Consultar',
  eliminar: 'Eliminar',
  ver_reportes: 'Ver reportes',
  aprobar_pagos: 'Aprobar pagos',
  marcar: 'Marcar ingreso y salida',
  ver_mis_marcaciones: 'Ver sus marcaciones',
  lectura: 'Lectura',
};

const CATALOGO: ModuloCatalogo[] = [
  { id: 'sedes', label: 'Sedes', acciones: [{ id: 'crear', label: 'Crear sedes' }] },
  {
    id: 'finanzas',
    label: 'Finanzas',
    acciones: [
      { id: 'ver_reportes', label: 'Ver reportes' },
      { id: 'aprobar_pagos', label: 'Aprobar pagos' },
    ],
  },
  {
    id: 'usuarios',
    label: 'Usuarios',
    acciones: [
      { id: 'leer', label: 'Consultar usuarios' },
      { id: 'crear', label: 'Crear y editar usuarios' },
      { id: 'eliminar', label: 'Eliminar usuarios' },
    ],
  },
  {
    id: 'asistencia',
    label: 'Asistencia',
    acciones: [
      { id: 'marcar', label: 'Marcar ingreso y salida' },
      { id: 'ver_mis_marcaciones', label: 'Ver sus marcaciones' },
    ],
  },
];

const PRESET = {
  ROLE_ADMIN: {
    sedes: { crear: true },
    finanzas: { ver_reportes: true, aprobar_pagos: true },
    usuarios: { leer: true, crear: true, eliminar: false },
  },
  ROLE_MULTIFUNCIONAL: {
    asistencia: { marcar: true, ver_mis_marcaciones: true },
  },
};

function etiqueta(clave: string, diccionario: Record<string, string>): string {
  const conocida = diccionario[clave];
  if (conocida) return conocida;
  const limpio = clave.replace(/[_-]+/g, ' ').trim();
  if (!limpio) return clave;
  return limpio.charAt(0).toUpperCase() + limpio.slice(1);
}

function esMapa(valor: unknown): valor is Record<string, unknown> {
  return !!valor && typeof valor === 'object' && !Array.isArray(valor);
}

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-roles-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, LoaderComponent, EmptyStateComponent],
  templateUrl: './roles-page.component.html',
  styleUrls: ['./roles-page.component.css'],
})
export class RolesPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.RolResponseDTO>();
  readonly form = this.fb.nonNullable.group({
    id: [''],
    nombre: ['ROLE_MULTIFUNCIONAL' as Api.RolRequestDTO['nombre'], Validators.required],
  });
  editor: GrupoEditor[] = [];
  modal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.RolResponseDTO | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: RolesApiService, private readonly lookup: LookupService, private readonly toast: ToastService) {}

  ngOnInit(): void { this.cargar(); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  cargar(): void {
    this.api.GetApiV1Roles().pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (rows) => {
        this.state.loading = false;
        this.state.rows = rows ?? [];
        this.state.totalElements = this.state.rows.length;
        this.state.totalPages = this.state.rows.length ? 1 : 0;
        this.state.error = null;
      },
      error: (error: unknown) => this.state.fail(problemText(error), null),
    });
  }

  nombreVisible(nombre: string | undefined): string {
    if (nombre === 'ROLE_ADMIN') return 'Administrador';
    if (nombre === 'ROLE_MULTIFUNCIONAL') return 'Multifuncional';
    return nombre ? etiqueta(nombre.replace(/^ROLE_/, ''), {}) : 'Rol';
  }

  descripcion(nombre: string | undefined): string {
    if (nombre === 'ROLE_ADMIN') return 'Administra usuarios, sedes y finanzas.';
    if (nombre === 'ROLE_MULTIFUNCIONAL') return 'Opera el día a día y marca su asistencia.';
    return 'Permisos asignados a este rol.';
  }

  gruposDe(rol: Api.RolResponseDTO): GrupoVisible[] {
    const permisos = rol.permisosJson;
    if (!esMapa(permisos)) return [];
    const grupos: GrupoVisible[] = [];
    for (const [modulo, valor] of Object.entries(permisos)) {
      const acciones = this.accionesActivas(valor);
      if (acciones.length) grupos.push({ label: etiqueta(modulo, MODULOS), acciones });
    }
    return grupos;
  }

  abrirCrear(): void {
    this.editando = false;
    this.errorFormulario = null;
    this.form.reset({ id: '', nombre: 'ROLE_MULTIFUNCIONAL' });
    this.editor = this.armarEditor(PRESET['ROLE_MULTIFUNCIONAL']);
    this.modal = true;
  }

  abrirEditar(row: Api.RolResponseDTO): void {
    this.editando = true;
    this.errorFormulario = null;
    const nombre = row.nombre === 'ROLE_ADMIN' ? 'ROLE_ADMIN' : 'ROLE_MULTIFUNCIONAL';
    this.form.reset({ id: row.id ?? '', nombre });
    this.editor = this.armarEditor(esMapa(row.permisosJson) ? row.permisosJson : {});
    this.modal = true;
  }

  alCambiarNombre(): void {
    if (this.editando) return;
    const nombre = this.form.controls.nombre.value;
    this.editor = this.armarEditor(nombre === 'ROLE_ADMIN' ? PRESET['ROLE_ADMIN'] : PRESET['ROLE_MULTIFUNCIONAL']);
  }

  alternar(accion: AccionEditor, marcado: boolean): void {
    accion.activo = marcado;
  }

  casillaMarcada(event: Event): boolean {
    return event.target instanceof HTMLInputElement && event.target.checked;
  }

  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const permisosJson = this.permisosParaGuardar();
    if (!permisosJson) {
      this.errorFormulario = 'Activa al menos un permiso.';
      return;
    }
    const value = this.form.getRawValue();
    const body: Api.RolRequestDTO = { nombre: value.nombre, permisosJson };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1RolesId({ id: value.id }, body) : this.api.PostApiV1Roles({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.lookup.invalidate('roles'); this.toast.success(this.editando ? 'Rol actualizado.' : 'Rol creado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }

  pedirEliminar(row: Api.RolResponseDTO): void { this.seleccionado = row; this.confirmar = true; }

  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1RolesId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.lookup.invalidate('roles'); this.toast.success('Rol eliminado.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }

  errorDeNombre(): string | null { return fieldError(this.form.controls.nombre); }

  private accionesActivas(valor: unknown): string[] {
    if (valor === true) return ['Acceso permitido'];
    if (Array.isArray(valor)) return valor.filter((item) => typeof item === 'string' && item.trim()).map((item) => etiqueta(String(item), ACCIONES));
    if (!esMapa(valor)) return [];
    return Object.entries(valor)
      .filter(([, activo]) => activo === true)
      .map(([accion]) => etiqueta(accion, ACCIONES));
  }

  private armarEditor(permisos: Record<string, unknown>): GrupoEditor[] {
    const extras = Object.keys(permisos).filter((id) => !CATALOGO.some((modulo) => modulo.id === id));
    const orden = [...CATALOGO.map((modulo) => modulo.id), ...extras];
    return orden.map((id) => this.grupoEditor(id, permisos));
  }

  private grupoEditor(id: string, permisos: Record<string, unknown>): GrupoEditor {
    const catalogo = CATALOGO.find((modulo) => modulo.id === id);
    const valor = permisos[id];
    const venia = Object.prototype.hasOwnProperty.call(permisos, id);
    const acciones: AccionEditor[] = [];
    if (typeof valor === 'boolean') {
      acciones.push({ id: '__acceso', label: 'Acceso permitido', activo: valor, forma: 'bool' });
      return { id, label: catalogo?.label ?? etiqueta(id, MODULOS), venia, acciones };
    }
    const mapa = esMapa(valor) ? valor : {};
    const lista = Array.isArray(valor) ? valor.filter((item): item is string => typeof item === 'string') : [];
    const vistos = new Set<string>();
    for (const accion of catalogo?.acciones ?? []) {
      vistos.add(accion.id);
      acciones.push({
        id: accion.id,
        label: accion.label,
        activo: mapa[accion.id] === true || lista.includes(accion.id),
        forma: 'mapa',
      });
    }
    for (const [accion, activo] of Object.entries(mapa)) {
      if (vistos.has(accion) || typeof activo !== 'boolean') continue;
      acciones.push({ id: accion, label: etiqueta(accion, ACCIONES), activo, forma: 'mapa' });
    }
    for (const accion of lista) {
      if (vistos.has(accion)) continue;
      acciones.push({ id: accion, label: etiqueta(accion, ACCIONES), activo: true, forma: 'mapa' });
    }
    return { id, label: catalogo?.label ?? etiqueta(id, MODULOS), venia, acciones };
  }

  private permisosParaGuardar(): Record<string, unknown> | null {
    const salida: Record<string, unknown> = {};
    let alguno = false;
    for (const grupo of this.editor) {
      if (grupo.acciones.length === 1 && grupo.acciones[0].forma === 'bool') {
        if (grupo.acciones[0].activo) {
          salida[grupo.id] = true;
          alguno = true;
        }
        continue;
      }
      const mapa: Record<string, boolean> = {};
      let activo = false;
      for (const accion of grupo.acciones) {
        if (accion.forma === 'bool') continue;
        mapa[accion.id] = accion.activo;
        if (accion.activo) activo = true;
      }
      if (activo || grupo.venia) {
        salida[grupo.id] = mapa;
        if (activo) alguno = true;
      }
    }
    return alguno ? salida : null;
  }
}

export const ROLES_ROUTES: Routes = [{
  path: '', component: RolesPageComponent, canActivate: [RoleGuard], title: 'Roles',
  data: { expectedRoles: ['ROLE_ADMIN'], section: 'Configuración', breadcrumb: 'Roles' },
}];
