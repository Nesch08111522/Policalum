import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { TabsComponent } from '../../../shared/components/tabs/tabs.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { ColumnDef, SelectOption, TabItem } from '../../../shared/models/ui.models';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemStatus, problemText } from '../../../shared/utils/feedback';
import { PageState } from '../../../shared/utils/page-state';
import { RrhhApiService } from '../data-access/rrhh.api';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-rrhh-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, TabsComponent, FilterBarComponent, DataTableComponent, ModalComponent, FormFieldComponent, SelectComponent, StatCardComponent, ConfirmDialogComponent],
  templateUrl: './rrhh-page.component.html',
  styleUrls: ['./rrhh-page.component.css'],
})
export class RrhhPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly tabs: TabItem[] = [
    { id: 'legajos', label: 'Legajos' },
    { id: 'asistencias', label: 'Asistencias' },
    { id: 'turnos', label: 'Turnos' },
    { id: 'nominas', label: 'Nóminas' },
    { id: 'comisiones', label: 'Comisiones' },
  ];
  tab = 'legajos';
  readonly legajos = new PageState<Api.LegajoListadoDTO>();
  readonly asistencias = new PageState<Api.RrhhAsistenciaResponseDTO>();
  readonly turnos = new PageState<Api.TurnoTrabajoResponseDTO>();
  readonly nominas = new PageState<Api.NominaListadoDTO>();
  readonly comisiones = new PageState<Api.ComisionResponseDTO>();
  readonly colLegajos: ColumnDef[] = [
    { key: 'empleadoNombre', label: 'Empleado' },
    { key: 'tipoContrato', label: 'Contrato', kind: 'badge' },
    { key: 'sistemaPension', label: 'Pensión' },
    { key: 'fechaIngreso', label: 'Ingreso', kind: 'date' },
    { key: 'sueldoBase', label: 'Sueldo', kind: 'money', align: 'end' },
    { key: 'activo', label: 'Activo', kind: 'bool' },
  ];
  readonly colAsistencias: ColumnDef[] = [
    { key: 'empleadoNombreCompleto', label: 'Empleado' },
    { key: 'tipoMarcacion', label: 'Marcación', kind: 'badge' },
    { key: 'fechaHora', label: 'Fecha', kind: 'date' },
    { key: 'dispositivoInfo', label: 'Dispositivo' },
  ];
  readonly colTurnos: ColumnDef[] = [
    { key: 'empleadoNombre', label: 'Empleado' },
    { key: 'nombreTurno', label: 'Turno' },
    { key: 'horaInicio', label: 'Inicio' },
    { key: 'horaFin', label: 'Fin' },
    { key: 'sedeNombre', label: 'Sede' },
    { key: 'activo', label: 'Activo', kind: 'bool' },
  ];
  readonly colNominas: ColumnDef[] = [
    { key: 'periodo', label: 'Periodo' },
    { key: 'empleadoNombre', label: 'Empleado' },
    { key: 'estado', label: 'Estado', kind: 'badge', badge: { PENDIENTE: 'warning', APROBADA: 'info', PAGADA: 'success' } },
    { key: 'totalNeto', label: 'Neto', kind: 'money', align: 'end' },
  ];
  readonly colComisiones: ColumnDef[] = [
    { key: 'periodo', label: 'Periodo' },
    { key: 'empleadoNombre', label: 'Empleado' },
    { key: 'estado', label: 'Estado', kind: 'badge', badge: { CALCULADA: 'warning', APROBADA: 'info', LIQUIDADA: 'success' } },
    { key: 'montoComision', label: 'Comisión', kind: 'money', align: 'end' },
  ];
  readonly filtroLegajo = this.fb.nonNullable.group({ sedeId: [''], tipoContrato: [''], sistemaPension: [''], activo: [''], texto: [''] });
  readonly filtroAsistencia = this.fb.nonNullable.group({ empleadoId: [''], fechaInicio: [''], fechaFin: [''] });
  readonly filtroTurno = this.fb.nonNullable.group({ sedeId: [''], userId: [''], activo: [''] });
  readonly filtroNomina = this.fb.nonNullable.group({ periodo: [''], sedeId: [''], estado: [''] });
  readonly filtroComision = this.fb.nonNullable.group({ periodo: [''], sedeId: [''], estado: [''] });
  readonly legajoForm = this.fb.nonNullable.group({
    empleadoId: ['', Validators.required],
    tipoContrato: ['PLANILLA', Validators.required],
    fechaIngreso: ['', Validators.required],
    sueldoBase: [0, [Validators.required, Validators.min(0.01)]],
    sistemaPension: ['ONP' as Api.LegajoRequestDTO['sistemaPension'], Validators.required],
    porcentajeComision: [0, [Validators.min(0), Validators.max(100)]],
    tasaPension: [0, [Validators.min(0), Validators.max(100)]],
    activo: [true],
  });
  readonly asistenciaForm = this.fb.nonNullable.group({
    empleadoId: ['', Validators.required],
    tipoMarcacion: ['ENTRADA' as NonNullable<Api.RrhhAsistenciaRequestDTO['tipoMarcacion']>, Validators.required],
    fechaHora: [''],
    dispositivoInfo: ['manual'],
  });
  readonly turnoForm = this.fb.nonNullable.group({
    id: [''],
    userId: ['', Validators.required],
    nombreTurno: ['', [Validators.required, Validators.maxLength(80)]],
    horaInicio: ['08:00', Validators.required],
    horaFin: ['17:00', Validators.required],
    diasSemana: ['LUNES,MARTES,MIERCOLES,JUEVES,VIERNES', Validators.required],
    toleranciaMinutos: [10, [Validators.min(0), Validators.max(120)]],
    sedeId: [''],
    activo: [true],
  });
  readonly nominaForm = this.fb.nonNullable.group({ periodo: ['', [Validators.required, Validators.pattern(/^\d{4}-\d{2}$/)]], sedeId: ['', Validators.required] });
  readonly pagoForm = this.fb.nonNullable.group({ metodoPago: ['TRANSFERENCIA' as Api.PagarNominaRequestDTO['metodoPago'], Validators.required], referencia: [''] });
  readonly comisionForm = this.fb.nonNullable.group({ periodo: ['', [Validators.required, Validators.pattern(/^\d{4}-\d{2}$/)]], sedeId: [''] });
  usuarios: SelectOption[] = [];
  sedes: SelectOption[] = [];
  asistenciaDash: Api.DashboardAsistenciaDTO | null = null;
  modal = false;
  pagoAbierto = false;
  confirmar = false;
  editando = false;
  guardando = false;
  errorFormulario: string | null = null;
  seleccionadoId = '';
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: RrhhApiService, private readonly lookup: LookupService, private readonly toast: ToastService) {}

  ngOnInit(): void {
    this.lookup.loadUsuarios().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.usuarios = this.lookup.options(rows, (row) => `${(row as Api.UserResponseDTO).nombre ?? ''} ${(row as Api.UserResponseDTO).apellido ?? ''}`.trim() || 'Usuario')));
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede')));
    this.cargar();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  cambiarTab(id: string): void { this.tab = id; this.cargar(); }

  cargar(): void {
    if (this.tab === 'legajos') this.cargarLegajos();
    if (this.tab === 'asistencias') this.cargarAsistencias();
    if (this.tab === 'turnos') this.cargarTurnos();
    if (this.tab === 'nominas') this.cargarNominas();
    if (this.tab === 'comisiones') this.cargarComisiones();
  }

  cargarLegajos(): void {
    this.legajos.loading = true;
    const filtros = this.filtroLegajo.getRawValue();
    this.api.GetApiV1RrhhLegajos(this.legajos.query({ ...filtros, activo: filtros.activo === '' ? undefined : filtros.activo === 'true' })).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => this.legajos.apply(page),
      error: (error: unknown) => this.legajos.fail(problemText(error), problemStatus(error)),
    });
  }

  cargarAsistencias(): void {
    this.asistencias.loading = true;
    this.api.GetApiV1RrhhAsistenciasDashboard().pipe(takeUntil(this.destroy$)).subscribe({ next: (data) => (this.asistenciaDash = data), error: () => (this.asistenciaDash = null) });
    this.api.GetApiV1RrhhAsistenciasListar(this.asistencias.query(this.filtroAsistencia.getRawValue())).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => this.asistencias.apply(page),
      error: (error: unknown) => this.asistencias.fail(problemText(error), problemStatus(error)),
    });
  }

  cargarTurnos(): void {
    this.turnos.loading = true;
    const filtros = this.filtroTurno.getRawValue();
    this.api.GetApiV1RrhhTurnos(this.turnos.query({ ...filtros, activo: filtros.activo === '' ? undefined : filtros.activo === 'true' })).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => this.turnos.apply(page),
      error: (error: unknown) => this.turnos.fail(problemText(error), problemStatus(error)),
    });
  }

  cargarNominas(): void {
    this.nominas.loading = true;
    this.api.GetApiV1RrhhNominas(this.nominas.query(this.filtroNomina.getRawValue())).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => this.nominas.apply(page),
      error: (error: unknown) => this.nominas.fail(problemText(error), problemStatus(error)),
    });
  }

  cargarComisiones(): void {
    this.comisiones.loading = true;
    this.api.GetApiV1RrhhComisiones(this.comisiones.query(this.filtroComision.getRawValue())).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => this.comisiones.apply(page),
      error: (error: unknown) => this.comisiones.fail(problemText(error), problemStatus(error)),
    });
  }

  filtrar(): void {
    const actual = this.estadoActual();
    actual.page = 0;
    this.cargar();
  }

  limpiar(): void {
    this.filtroLegajo.reset({ sedeId: '', tipoContrato: '', sistemaPension: '', activo: '', texto: '' });
    this.filtroAsistencia.reset({ empleadoId: '', fechaInicio: '', fechaFin: '' });
    this.filtroTurno.reset({ sedeId: '', userId: '', activo: '' });
    this.filtroNomina.reset({ periodo: '', sedeId: '', estado: '' });
    this.filtroComision.reset({ periodo: '', sedeId: '', estado: '' });
    this.filtrar();
  }

  cambiarPagina(page: number): void { this.estadoActual().page = page; this.cargar(); }
  cambiarTamano(size: number): void { const actual = this.estadoActual(); actual.size = size; actual.page = 0; this.cargar(); }

  abrirCrear(): void {
    this.editando = false;
    this.errorFormulario = null;
    this.legajoForm.reset({ empleadoId: '', tipoContrato: 'PLANILLA', fechaIngreso: '', sueldoBase: 0, sistemaPension: 'ONP', porcentajeComision: 0, tasaPension: 0, activo: true });
    this.asistenciaForm.reset({ empleadoId: '', tipoMarcacion: 'ENTRADA', fechaHora: '', dispositivoInfo: 'manual' });
    this.turnoForm.reset({ id: '', userId: '', nombreTurno: '', horaInicio: '08:00', horaFin: '17:00', diasSemana: 'LUNES,MARTES,MIERCOLES,JUEVES,VIERNES', toleranciaMinutos: 10, sedeId: '', activo: true });
    this.nominaForm.reset({ periodo: new Date().toISOString().slice(0, 7), sedeId: '' });
    this.comisionForm.reset({ periodo: new Date().toISOString().slice(0, 7), sedeId: '' });
    this.modal = true;
  }

  editarLegajo(row: unknown): void {
    const item = row as Api.LegajoListadoDTO;
    if (!item.empleadoId) return;
    this.api.GetApiV1RrhhLegajosEmpleadoId({ empleadoId: item.empleadoId }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.editando = true;
        this.legajoForm.reset({
          empleadoId: data.empleadoId ?? '',
          tipoContrato: data.tipoContrato ?? 'PLANILLA',
          fechaIngreso: (data.fechaIngreso ?? '').slice(0, 10),
          sueldoBase: data.sueldoBase ?? 0,
          sistemaPension: data.sistemaPension ?? 'ONP',
          porcentajeComision: data.porcentajeComision ?? 0,
          tasaPension: data.tasaPension ?? 0,
          activo: data.activo ?? true,
        });
        this.legajoForm.controls.empleadoId.disable();
        this.modal = true;
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  editarTurno(row: unknown): void {
    const item = row as Api.TurnoTrabajoResponseDTO;
    this.editando = true;
    this.turnoForm.reset({
      id: item.id ?? '', userId: item.userId ?? '', nombreTurno: item.nombreTurno ?? '', horaInicio: (item.horaInicio ?? '08:00').slice(0, 5), horaFin: (item.horaFin ?? '17:00').slice(0, 5),
      diasSemana: (item.diasSemana ?? []).join(','), toleranciaMinutos: item.toleranciaMinutos ?? 10, sedeId: item.sedeId ?? '', activo: item.activo ?? true,
    });
    this.modal = true;
  }

  guardar(): void {
    if (this.tab === 'legajos') this.guardarLegajo();
    else if (this.tab === 'asistencias') this.guardarAsistencia();
    else if (this.tab === 'turnos') this.guardarTurno();
    else if (this.tab === 'nominas') this.generarNomina();
    else this.calcularComisiones();
  }

  guardarLegajo(): void {
    this.legajoForm.markAllAsTouched();
    if (this.legajoForm.invalid) return;
    const value = this.legajoForm.getRawValue();
    const body: Api.LegajoRequestDTO = {
      empleadoId: value.empleadoId, tipoContrato: value.tipoContrato, fechaIngreso: value.fechaIngreso, sueldoBase: Number(value.sueldoBase),
      sistemaPension: value.sistemaPension, porcentajeComision: Number(value.porcentajeComision), tasaPension: Number(value.tasaPension), activo: value.activo,
    };
    this.guardando = true;
    const request = this.editando ? this.api.PutApiV1RrhhLegajosEmpleadoId({ empleadoId: value.empleadoId }, body) : this.api.PostApiV1RrhhLegajos({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.ok('Legajo guardado.'),
      error: (error: unknown) => this.fail(error, this.legajoForm),
    });
  }

  guardarAsistencia(): void {
    this.asistenciaForm.markAllAsTouched();
    if (this.asistenciaForm.invalid) return;
    const value = this.asistenciaForm.getRawValue();
    this.guardando = true;
    this.api.PostApiV1RrhhAsistenciasManual({}, { empleadoId: value.empleadoId, tipoMarcacion: value.tipoMarcacion, fechaHora: value.fechaHora || undefined, dispositivoInfo: value.dispositivoInfo || 'manual' }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => this.ok('Marcación registrada.'),
      error: (error: unknown) => this.fail(error, this.asistenciaForm),
    });
  }

  guardarTurno(): void {
    this.turnoForm.markAllAsTouched();
    if (this.turnoForm.invalid) return;
    const value = this.turnoForm.getRawValue();
    const body: Api.TurnoTrabajoRequestDTO = {
      userId: value.userId, nombreTurno: value.nombreTurno.trim(), horaInicio: value.horaInicio, horaFin: value.horaFin,
      diasSemana: value.diasSemana.split(',').map((dia) => dia.trim()).filter(Boolean),
      toleranciaMinutos: Number(value.toleranciaMinutos), sedeId: value.sedeId || undefined, activo: value.activo,
    };
    this.guardando = true;
    const request = this.editando && value.id ? this.api.PutApiV1RrhhTurnosId({ id: value.id }, body) : this.api.PostApiV1RrhhTurnos({}, body);
    request.pipe(takeUntil(this.destroy$)).subscribe({ next: () => this.ok('Turno guardado.'), error: (error: unknown) => this.fail(error, this.turnoForm) });
  }

  generarNomina(): void {
    this.nominaForm.markAllAsTouched();
    if (this.nominaForm.invalid) return;
    const value = this.nominaForm.getRawValue();
    this.guardando = true;
    this.api.PostApiV1RrhhNominasGenerar({}, { periodo: value.periodo, sedeId: value.sedeId }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => this.ok(`Nóminas generadas: ${data.generadas ?? 0}.`),
      error: (error: unknown) => this.fail(error, this.nominaForm),
    });
  }

  calcularComisiones(): void {
    this.comisionForm.markAllAsTouched();
    if (this.comisionForm.invalid) return;
    const value = this.comisionForm.getRawValue();
    this.guardando = true;
    this.api.PostApiV1RrhhComisionesCalcular({}, { periodo: value.periodo, sedeId: value.sedeId || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => this.ok(`Comisiones nuevas: ${data.nuevas ?? 0}.`),
      error: (error: unknown) => this.fail(error, this.comisionForm),
    });
  }

  aprobarNomina(row: unknown): void {
    const item = row as Api.NominaListadoDTO;
    if (!item.id) return;
    this.api.PostApiV1RrhhNominasIdAprobar({ id: item.id }).pipe(takeUntil(this.destroy$)).subscribe({ next: () => { this.toast.success('Nómina aprobada.'); this.cargarNominas(); }, error: (error: unknown) => this.toast.error(problemText(error)) });
  }

  abrirPago(row: unknown): void { this.seleccionadoId = (row as Api.NominaListadoDTO).id ?? ''; this.pagoForm.reset({ metodoPago: 'TRANSFERENCIA', referencia: '' }); this.pagoAbierto = true; }
  pagar(): void {
    if (!this.seleccionadoId || this.pagoForm.invalid) return;
    const value = this.pagoForm.getRawValue();
    this.guardando = true;
    this.api.PostApiV1RrhhNominasIdPagar({ id: this.seleccionadoId }, { metodoPago: value.metodoPago, referencia: value.referencia.trim() || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.pagoAbierto = false; this.toast.success('Nómina pagada.'); this.cargarNominas(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  aprobarComision(row: unknown): void {
    const item = row as Api.ComisionResponseDTO;
    if (!item.id) return;
    this.api.PatchApiV1RrhhComisionesIdAprobar({ id: item.id }).pipe(takeUntil(this.destroy$)).subscribe({ next: () => { this.toast.success('Comisión aprobada.'); this.cargarComisiones(); }, error: (error: unknown) => this.toast.error(problemText(error)) });
  }

  pedirEliminar(row: unknown): void {
    const item = row as { id?: string };
    this.seleccionadoId = item.id ?? '';
    this.confirmar = true;
  }

  eliminar(): void {
    if (!this.seleccionadoId) return;
    this.guardando = true;
    const request = this.tab === 'turnos' ? this.api.DeleteApiV1RrhhTurnosId({ id: this.seleccionadoId }) : this.api.DeleteApiV1RrhhNominasId({ id: this.seleccionadoId });
    request.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.confirmar = false; this.toast.success('Registro eliminado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.confirmar = false; this.toast.error(problemText(error)); },
    });
  }

  estadoPendiente(row: unknown): boolean { return (row as Api.NominaListadoDTO).estado === 'PENDIENTE'; }
  estadoAprobada(row: unknown): boolean { return (row as Api.NominaListadoDTO).estado === 'APROBADA'; }
  comisionCalculada(row: unknown): boolean { return (row as Api.ComisionResponseDTO).estado === 'CALCULADA'; }
  tituloAccion(): string {
    if (this.tab === 'asistencias') return 'Marcación manual';
    if (this.tab === 'turnos') return this.editando ? 'Editar turno' : 'Nuevo turno';
    if (this.tab === 'nominas') return 'Generar nómina';
    if (this.tab === 'comisiones') return 'Calcular comisiones';
    return this.editando ? 'Editar legajo' : 'Nuevo legajo';
  }

  accionLabel(): string {
    if (this.tab === 'asistencias') return 'Nueva marcación';
    if (this.tab === 'turnos') return 'Nuevo turno';
    if (this.tab === 'nominas') return 'Generar';
    if (this.tab === 'comisiones') return 'Calcular';
    return 'Nuevo legajo';
  }

  formularioInvalido(): boolean {
    if (this.tab === 'legajos') return this.legajoForm.invalid;
    if (this.tab === 'asistencias') return this.asistenciaForm.invalid;
    if (this.tab === 'turnos') return this.turnoForm.invalid;
    if (this.tab === 'nominas') return this.nominaForm.invalid;
    return this.comisionForm.invalid;
  }

  errorDe(nombre: 'empleadoId' | 'fechaIngreso' | 'sueldoBase'): string | null { return fieldError(this.legajoForm.controls[nombre]); }

  private estadoActual(): { page: number; size: number } {
    if (this.tab === 'asistencias') return this.asistencias;
    if (this.tab === 'turnos') return this.turnos;
    if (this.tab === 'nominas') return this.nominas;
    if (this.tab === 'comisiones') return this.comisiones;
    return this.legajos;
  }

  private ok(mensaje: string): void {
    this.guardando = false;
    this.modal = false;
    this.legajoForm.controls.empleadoId.enable();
    this.toast.success(mensaje);
    this.cargar();
  }

  private fail(error: unknown, form: FormGroup): void {
    this.guardando = false;
    applyServerErrors(form, error);
    this.errorFormulario = problemText(error);
  }
}

export const RRHH_ROUTES: Routes = [
  { path: '', component: RrhhPageComponent, title: 'Recursos humanos', data: { section: 'Personas', breadcrumb: 'Recursos humanos' } },
];
