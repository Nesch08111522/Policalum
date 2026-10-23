import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { RoleGuard } from '../../../guards/role.guard';
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
import { applyServerErrors, fieldError, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { UsersApiService } from '../data-access/users.api';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-users-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent, SelectComponent],
  templateUrl: './users-page.component.html',
  styleUrls: ['./users-page.component.css'],
})
export class UsersPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.UserResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'nombreUsuario', label: 'Usuario' },
    { key: 'nombre', label: 'Nombre' },
    { key: 'apellido', label: 'Apellido' },
    { key: 'email', label: 'Correo' },
    { key: 'rol.nombre', label: 'Rol' },
    { key: 'sede.nombre', label: 'Sede' },
    { key: 'activo', label: 'Activo', kind: 'bool' },
  ];
  readonly filtros = this.fb.nonNullable.group({ texto: [''] });
  readonly form = this.fb.nonNullable.group({
    id: [''],
    nombreUsuario: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(20), Validators.pattern(/^[a-zA-Z0-9_]+$/)]],
    nombre: ['', [Validators.required, Validators.maxLength(50)]],
    apellido: ['', [Validators.required, Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    celular: ['', [Validators.required, Validators.pattern(/^9\d{8}$/)]],
    passwordHash: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_]).*$/)]],
    rolId: ['', Validators.required],
    sedeId: ['', Validators.required],
    activo: [true],
  });
  roles: SelectOption[] = [];
  sedes: SelectOption[] = [];
  private todos: Api.UserResponseDTO[] = [];
  modal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.UserResponseDTO | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: UsersApiService, private readonly lookup: LookupService, private readonly toast: ToastService) {}
  ngOnInit(): void {
    this.cargar();
    this.lookup.loadRoles().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.roles = rows.filter((row) => row.id).map((row) => ({ value: row.id ?? '', label: row.nombre ?? 'Rol' })); });
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede'); });
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  cargar(): void {
    this.api.GetApiV1Users().pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (rows) => { this.todos = rows ?? []; this.state.loading = false; this.paginar(); },
      error: (error: unknown) => { this.state.loading = false; this.state.fail(problemText(error), null); },
    });
  }
  paginar(): void {
    const texto = this.filtros.controls.texto.value.trim().toLowerCase();
    const filtrados = this.todos.filter((user) => !texto || `${user.nombreUsuario} ${user.nombre} ${user.apellido} ${user.email}`.toLowerCase().includes(texto));
    this.state.totalElements = filtrados.length;
    this.state.totalPages = filtrados.length ? Math.ceil(filtrados.length / this.state.size) : 0;
    this.state.rows = filtrados.slice(this.state.page * this.state.size, (this.state.page + 1) * this.state.size);
  }
  filtrar(): void { this.state.page = 0; this.paginar(); }
  limpiar(): void { this.filtros.reset(); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.paginar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.paginar(); }
  abrirCrear(): void { this.editando = false; this.errorFormulario = null; this.form.reset({ id: '', nombreUsuario: '', nombre: '', apellido: '', email: '', celular: '', passwordHash: '', rolId: '', sedeId: '', activo: true }); this.modal = true; }
  abrirEditar(row: unknown): void {
    const user = row as Api.UserResponseDTO;
    if (!user.id) { this.toast.warning('Este usuario no trae identificador. Regenera el cliente OpenAPI o despliega el backend actualizado.'); return; }
    this.editando = true;
    this.errorFormulario = null;
    this.form.reset({ id: user.id, nombreUsuario: user.nombreUsuario ?? '', nombre: user.nombre ?? '', apellido: user.apellido ?? '', email: user.email ?? '', celular: user.celular ?? '', passwordHash: '', rolId: user.rol?.id ?? '', sedeId: user.sede?.id ?? '', activo: user.activo ?? true });
    this.modal = true;
  }
  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.UserRequestDTO = {
      nombreUsuario: value.nombreUsuario.trim(), nombre: value.nombre.trim(), apellido: value.apellido.trim(), email: value.email.trim(),
      celular: value.celular.trim(), passwordHash: value.passwordHash, activo: value.activo,
      rol: { id: value.rolId }, sede: { id: value.sedeId },
    };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1UsersId({ id: value.id }, body) : this.api.PostApiV1Users({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.lookup.invalidate('usuarios'); this.toast.success(this.editando ? 'Usuario actualizado.' : 'Usuario creado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }
  pedirEliminar(row: unknown): void {
    const user = row as Api.UserResponseDTO;
    if (!user.id) { this.toast.warning('No se puede eliminar sin el identificador del usuario.'); return; }
    this.seleccionado = user;
    this.confirmar = true;
  }
  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1UsersId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.lookup.invalidate('usuarios'); this.toast.success('Usuario eliminado.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }
  errorDe(nombre: 'nombreUsuario' | 'nombre' | 'apellido' | 'email' | 'celular' | 'passwordHash' | 'rolId' | 'sedeId'): string | null {
    return fieldError(this.form.controls[nombre], {
      pattern: nombre === 'celular' ? 'Debe tener 9 dígitos y empezar con 9.' : nombre === 'nombreUsuario' ? 'Solo letras, números y guion bajo.' : 'Debe incluir mayúscula, minúscula, número y un símbolo @#$%^&+=!_.',
    });
  }
}

export const USERS_ROUTES: Routes = [{
  path: '', component: UsersPageComponent, canActivate: [RoleGuard], title: 'Usuarios',
  data: { expectedRoles: ['ROLE_ADMIN'], section: 'Configuración', breadcrumb: 'Usuarios' },
}];
