import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { ColumnDef } from '../../../shared/models/ui.models';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { documentoCliente } from '../../../shared/utils/validators';
import * as Api from '../../../core/api/openapi.types';
import { ClientesApiService } from '../data-access/clientes.api';

@Component({
  selector: 'app-clientes-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent],
  templateUrl: './clientes-page.component.html',
  styleUrls: ['./clientes-page.component.css'],
})
export class ClientesPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.ClienteResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'nombre', label: 'Nombre', sortable: true },
    { key: 'tipoDocumento', label: 'Tipo', kind: 'badge', badge: { DNI: 'info', RUC: 'success', CE: 'warning', PASAPORTE: 'neutral' } },
    { key: 'numeroDocumento', label: 'Documento' },
    { key: 'celular', label: 'Celular' },
    { key: 'email', label: 'Correo' },
    { key: 'createdAt', label: 'Registro', kind: 'date', sortable: true },
  ];
  readonly filtros = this.fb.nonNullable.group({ searchTerm: [''], tipoDocumento: [''] });
  readonly form = this.fb.nonNullable.group({
    id: [''],
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    tipoDocumento: ['DNI' as Api.ClienteRequestDTO['tipoDocumento'], Validators.required],
    numeroDocumento: ['', [Validators.required, documentoCliente]],
    celular: ['', Validators.pattern(/^$|^9\d{8}$/)],
    email: ['', Validators.email],
    direccion: ['', Validators.maxLength(255)],
  });
  modal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.ClienteResponseDTO | null = null;
  private readonly buscar$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: ClientesApiService, private readonly lookup: LookupService, private readonly toast: ToastService) {
    this.state.sort = 'nombre,asc';
  }

  ngOnInit(): void {
    this.cargar();
    this.buscar$.pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe((valor) => {
      this.filtros.controls.searchTerm.setValue(valor);
      this.filtrar();
    });
    this.form.controls.tipoDocumento.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.form.controls.numeroDocumento.updateValueAndValidity();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  cargar(): void {
    this.api.GetApiV1Clientes(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }

  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ searchTerm: '', tipoDocumento: '' }); this.filtrar(); }
  onBuscar(event: Event): void { this.buscar$.next((event.target as HTMLInputElement).value); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void {
    this.state.sort = `${event.key},${event.direction}`;
    this.cargar();
  }

  abrirCrear(): void {
    this.editando = false;
    this.errorFormulario = null;
    this.form.reset({ id: '', nombre: '', tipoDocumento: 'DNI', numeroDocumento: '', celular: '', email: '', direccion: '' });
    this.modal = true;
  }

  abrirEditar(row: unknown): void {
    const cliente = row as Api.ClienteResponseDTO;
    this.editando = true;
    this.errorFormulario = null;
    this.form.reset({
      id: cliente.id ?? '',
      nombre: cliente.nombre ?? '',
      tipoDocumento: cliente.tipoDocumento ?? 'DNI',
      numeroDocumento: cliente.numeroDocumento ?? '',
      celular: cliente.celular ?? '',
      email: cliente.email ?? '',
      direccion: cliente.direccion ?? '',
    });
    this.modal = true;
  }

  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.ClienteRequestDTO = {
      nombre: value.nombre.trim(),
      tipoDocumento: value.tipoDocumento,
      numeroDocumento: value.numeroDocumento.trim(),
      celular: value.celular.trim() || undefined,
      email: value.email.trim() || undefined,
      direccion: value.direccion.trim() || undefined,
    };
    this.guardando = true;
    this.errorFormulario = null;
    const request = this.editando && value.id
      ? this.api.PutApiV1ClientesId({ id: value.id }, body)
      : this.api.PostApiV1Clientes({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.guardando = false;
        this.modal = false;
        this.lookup.invalidate('clientes');
        this.toast.success(this.editando ? 'Cliente actualizado.' : 'Cliente creado.');
        this.cargar();
      },
      error: (error: unknown) => {
        this.guardando = false;
        applyServerErrors(this.form, error);
        this.errorFormulario = problemText(error);
      },
    });
  }

  pedirEliminar(row: unknown): void {
    this.seleccionado = row as Api.ClienteResponseDTO;
    this.confirmar = true;
  }

  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1ClientesId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.eliminando = false;
        this.confirmar = false;
        this.lookup.invalidate('clientes');
        this.toast.success('Cliente eliminado.');
        this.cargar();
      },
      error: (error: unknown) => {
        this.eliminando = false;
        this.confirmar = false;
        this.toast.error(problemText(error));
      },
    });
  }

  errorDe(nombre: keyof typeof this.form.controls): string | null {
    return fieldError(this.form.controls[nombre], { pattern: 'El celular debe tener 9 dígitos y empezar con 9.' });
  }
}

export const CLIENTES_ROUTES: Routes = [
  { path: '', component: ClientesPageComponent, title: 'Clientes', data: { section: 'Ventas', breadcrumb: 'Clientes' } },
];
