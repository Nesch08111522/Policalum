import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
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
import { ProveedoresApiService } from '../data-access/proveedores.api';

@Component({
  selector: 'app-proveedores-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, DataTableComponent, ModalComponent, ConfirmDialogComponent, FormFieldComponent],
  templateUrl: './proveedores-page.component.html',
  styleUrls: ['./proveedores-page.component.css'],
})
export class ProveedoresPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.ProveedorResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'nombreRazon', label: 'Razón social', sortable: true },
    { key: 'tipoDocumento', label: 'Tipo' },
    { key: 'numeroDocumento', label: 'Documento' },
    { key: 'celular', label: 'Celular' },
    { key: 'diasCredito', label: 'Días crédito', kind: 'number', align: 'end' },
    { key: 'activo', label: 'Activo', kind: 'bool' },
  ];
  readonly filtros = this.fb.nonNullable.group({ searchTerm: [''], activo: [''] });
  readonly form = this.fb.nonNullable.group({
    id: [''],
    nombreRazon: ['', [Validators.required, Validators.maxLength(200)]],
    tipoDocumento: ['RUC', Validators.required],
    numeroDocumento: ['', [Validators.required, Validators.maxLength(20)]],
    celular: [''],
    email: ['', Validators.email],
    direccion: [''],
    banco: [''],
    cuentaBancaria: [''],
    condicionesPago: [''],
    diasCredito: [0, [Validators.min(0)]],
    activo: [true],
  });
  modal = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  confirmar = false;
  eliminando = false;
  seleccionado: Api.ProveedorResponseDTO | null = null;
  private readonly buscar$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: ProveedoresApiService, private readonly lookup: LookupService, private readonly toast: ToastService) { this.state.sort = 'nombreRazon,asc'; }
  ngOnInit(): void {
    this.cargar();
    this.buscar$.pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe((valor) => { this.filtros.controls.searchTerm.setValue(valor); this.filtrar(); });
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  cargar(): void {
    this.api.GetApiV1Proveedores(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }
  filtrar(): void { this.state.page = 0; this.cargar(); }
  limpiar(): void { this.filtros.reset({ searchTerm: '', activo: '' }); this.filtrar(); }
  onBuscar(event: Event): void { this.buscar$.next((event.target as HTMLInputElement).value); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }
  abrirCrear(): void { this.editando = false; this.errorFormulario = null; this.form.reset({ id: '', nombreRazon: '', tipoDocumento: 'RUC', numeroDocumento: '', celular: '', email: '', direccion: '', banco: '', cuentaBancaria: '', condicionesPago: '', diasCredito: 0, activo: true }); this.modal = true; }
  abrirEditar(row: unknown): void {
    const item = row as Api.ProveedorResponseDTO;
    this.editando = true;
    this.errorFormulario = null;
    this.form.reset({ id: item.id ?? '', nombreRazon: item.nombreRazon ?? '', tipoDocumento: item.tipoDocumento ?? 'RUC', numeroDocumento: item.numeroDocumento ?? '', celular: item.celular ?? '', email: item.email ?? '', direccion: item.direccion ?? '', banco: item.banco ?? '', cuentaBancaria: item.cuentaBancaria ?? '', condicionesPago: item.condicionesPago ?? '', diasCredito: item.diasCredito ?? 0, activo: item.activo ?? true });
    this.modal = true;
  }
  guardar(): void {
    if (this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    const body: Api.ProveedorRequestDTO = {
      nombreRazon: value.nombreRazon.trim(), tipoDocumento: value.tipoDocumento, numeroDocumento: value.numeroDocumento.trim(),
      celular: value.celular.trim() || undefined, email: value.email.trim() || undefined, direccion: value.direccion.trim() || undefined,
      banco: value.banco.trim() || undefined, cuentaBancaria: value.cuentaBancaria.trim() || undefined, condicionesPago: value.condicionesPago.trim() || undefined,
      diasCredito: Number(value.diasCredito), activo: value.activo,
    };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1ProveedoresId({ id: value.id }, body) : this.api.PostApiV1Proveedores({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.modal = false; this.lookup.invalidate('proveedores'); this.toast.success(this.editando ? 'Proveedor actualizado.' : 'Proveedor creado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.form, error); this.errorFormulario = problemText(error); },
    });
  }
  pedirEliminar(row: unknown): void { this.seleccionado = row as Api.ProveedorResponseDTO; this.confirmar = true; }
  eliminar(): void {
    if (!this.seleccionado?.id) return;
    this.eliminando = true;
    this.api.DeleteApiV1ProveedoresId({ id: this.seleccionado.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.eliminando = false; this.confirmar = false; this.lookup.invalidate('proveedores'); this.toast.success('Proveedor eliminado.'); this.cargar(); },
      error: (error: unknown) => { this.eliminando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }
  errorDe(nombre: 'nombreRazon' | 'numeroDocumento' | 'email' | 'diasCredito'): string | null { return fieldError(this.form.controls[nombre]); }
}

export const PROVEEDORES_ROUTES: Routes = [
  { path: '', component: ProveedoresPageComponent, title: 'Proveedores', data: { section: 'Compras', breadcrumb: 'Proveedores' } },
];
