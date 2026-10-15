import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { RoleGuard } from '../../../guards/role.guard';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { ColumnDef } from '../../../shared/models/ui.models';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemText } from '../../../shared/utils/feedback';
import { PageState } from '../../../shared/utils/page-state';
import { jsonObject } from '../../../shared/utils/validators';
import { RolesApiService } from '../data-access/roles.api';

@Component({
  selector: 'app-roles-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent],
  templateUrl: './roles-page.component.html',
  styleUrls: ['./roles-page.component.css'],
})
export class RolesPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.RolResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'nombre', label: 'Rol' },
    { key: 'permisosJson', label: 'Permisos' },
  ];
  readonly form = this.fb.nonNullable.group({
    id: [''],
    nombre: ['ROLE_MULTIFUNCIONAL' as Api.RolRequestDTO['nombre'], Validators.required],
    permisosJson: ['{\n  "modulos": ["lectura"]\n}', [Validators.required, jsonObject]],
  });
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
    this.state.loading = true;
    this.api.GetApiV1Roles().pipe(takeUntil(this.destroy$)).subscribe({
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
  abrirCrear(): void { this.editando = false; this.errorFormulario = null; this.form.reset({ id: '', nombre: 'ROLE_MULTIFUNCIONAL', permisosJson: '{\n  "modulos": ["lectura"]\n}' }); this.modal = true; }
  abrirEditar(row: unknown): void {
    const rol = row as Api.RolResponseDTO;
    this.editando = true;
    this.errorFormulario = null;
    const nombre = rol.nombre === 'ROLE_ADMIN' ? 'ROLE_ADMIN' : 'ROLE_MULTIFUNCIONAL';
    this.form.reset({ id: rol.id ?? '', nombre, permisosJson: JSON.stringify(rol.permisosJson ?? {}, null, 2) });
    this.modal = true;
  }
  guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.RolRequestDTO = { nombre: value.nombre, permisosJson: JSON.parse(value.permisosJson) as Record<string, unknown> };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1RolesId({ id: value.id }, body) : this.api.PostApiV1Roles({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.lookup.roles = []; this.toast.success(this.editando ? 'Rol actualizado.' : 'Rol creado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }
  pedirEliminar(row: unknown): void { this.seleccionado = row as Api.RolResponseDTO; this.confirmar = true; }
  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1RolesId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.lookup.roles = []; this.toast.success('Rol eliminado.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }
  errorDe(nombre: 'nombre' | 'permisosJson'): string | null { return fieldError(this.form.controls[nombre]); }
}

export const ROLES_ROUTES: Routes = [{
  path: '', component: RolesPageComponent, canActivate: [RoleGuard], title: 'Roles',
  data: { expectedRoles: ['ROLE_ADMIN'], section: 'Configuración', breadcrumb: 'Roles' },
}];
