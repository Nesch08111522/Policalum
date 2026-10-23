import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { RoleGuard } from '../../../guards/role.guard';
import { BarChartComponent, ChartPoint } from '../../../shared/components/bar-chart/bar-chart.component';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FilterBarComponent } from '../../../shared/components/filter-bar/filter-bar.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { StatCardComponent } from '../../../shared/components/stat-card/stat-card.component';
import { ColumnDef, SelectOption } from '../../../shared/models/ui.models';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { problemStatus, problemText } from '../../../shared/utils/feedback';
import { ignoreStale, PageState } from '../../../shared/utils/page-state';
import { AuditoriaApiService } from '../data-access/auditoria.api';

function haceDias(dias: number): string {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() - dias);
  return fecha.toISOString().slice(0, 10);
}

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-auditoria-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FilterBarComponent, FormFieldComponent, SelectComponent, DataTableComponent, StatCardComponent, BarChartComponent, ModalComponent],
  templateUrl: './auditoria-page.component.html',
  styleUrls: ['./auditoria-page.component.css'],
})
export class AuditoriaPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly state = new PageState<Api.LogAuditoriaResponseDTO>();
  readonly columns: ColumnDef[] = [
    { key: 'createdAt', label: 'Fecha', kind: 'date', sortable: true },
    { key: 'accion', label: 'Acción', kind: 'badge', badge: { INSERT: 'success', UPDATE: 'info', DELETE: 'danger', LOGIN: 'neutral', LOGOUT: 'neutral', ACCESS: 'warning' } },
    { key: 'tabla', label: 'Tabla' },
    { key: 'userNombre', label: 'Usuario' },
    { key: 'registroId', label: 'Registro' },
  ];
  readonly filtros = this.fb.nonNullable.group({ tabla: [''], accion: [''], userId: [''], desde: [haceDias(7)], hasta: [new Date().toISOString().slice(0, 10)] });
  usuarios: SelectOption[] = [];
  dashboard: Api.AuditoriaDashboardDTO | null = null;
  puntos: ChartPoint[] = [];
  detalle: Api.LogAuditoriaResponseDTO | null = null;
  detalleJson = '';
  abierto = false;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: AuditoriaApiService, private readonly lookup: LookupService, private readonly toast: ToastService) {
    this.state.sort = 'createdAt,desc';
  }

  ngOnInit(): void {
    this.lookup.loadUsuarios().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.usuarios = this.lookup.options(rows, (row) => (row as Api.UserResponseDTO).email ?? 'Usuario')));
    this.cargar();
    this.cargarDashboard();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  cargar(): void {
    this.api.GetApiV1AuditoriaLogs(this.state.query(this.filtros.getRawValue())).pipe(ignoreStale(this.state), takeUntil(this.destroy$)).subscribe({
      next: (page) => this.state.apply(page),
      error: (error: unknown) => this.state.fail(problemText(error), problemStatus(error)),
    });
  }

  cargarDashboard(): void {
    const filtros = this.filtros.getRawValue();
    this.api.GetApiV1AuditoriaDashboard({ desde: filtros.desde, hasta: filtros.hasta }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.dashboard = data;
        this.puntos = (data.eventosPorDia ?? []).map((dia) => ({ label: (dia.fecha ?? '').slice(5), value: Number(dia.cantidad ?? 0) }));
      },
      error: () => { this.dashboard = null; this.puntos = []; },
    });
  }

  filtrar(): void { this.state.page = 0; this.cargar(); this.cargarDashboard(); }
  limpiar(): void { this.filtros.reset({ tabla: '', accion: '', userId: '', desde: haceDias(7), hasta: new Date().toISOString().slice(0, 10) }); this.filtrar(); }
  cambiarPagina(page: number): void { this.state.page = page; this.cargar(); }
  cambiarTamano(size: number): void { this.state.size = size; this.state.page = 0; this.cargar(); }
  ordenar(event: { key: string; direction: 'asc' | 'desc' }): void { this.state.sort = `${event.key},${event.direction}`; this.cargar(); }

  ver(row: unknown): void {
    const item = row as Api.LogAuditoriaResponseDTO;
    if (!item.id) return;
    this.api.GetApiV1AuditoriaLogsId({ id: item.id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => { this.detalle = data; this.detalleJson = JSON.stringify(data.datosJson ?? {}, null, 2); this.abierto = true; },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }
}

export const AUDITORIA_ROUTES: Routes = [{
  path: '',
  component: AuditoriaPageComponent,
  canActivate: [RoleGuard],
  title: 'Auditoría',
  data: { expectedRoles: ['ROLE_ADMIN'], section: 'Análisis', breadcrumb: 'Auditoría' },
}];
