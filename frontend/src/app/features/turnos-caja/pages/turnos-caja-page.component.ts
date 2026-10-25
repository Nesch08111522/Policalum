import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit, effect, inject, untracked, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { SelectOption } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { CajaSessionService } from '../../../shared/services/caja-session.service';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { problemText } from '../../../shared/utils/feedback';
import { TurnosCajaApiService } from '../data-access/turnos-caja.api';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-turnos-caja-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FormFieldComponent, SelectComponent, PenPipe],
  templateUrl: './turnos-caja-page.component.html',
  styleUrls: ['./turnos-caja-page.component.css'],
})
export class TurnosCajaPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly caja = inject(CajaSessionService);
  readonly apertura = this.fb.nonNullable.group({
    sedeId: ['', Validators.required],
    montoInicial: [0, [Validators.required, Validators.min(0)]],
    observaciones: [''],
  });
  readonly cierre = this.fb.nonNullable.group({
    id: ['', Validators.required],
    montoFinalDeclarado: [0, [Validators.required, Validators.min(0)]],
    observaciones: [''],
  });
  readonly cajaChica = this.fb.nonNullable.group({
    tipo: ['EGRESO', Validators.required],
    concepto: ['TRANSPORTE', Validators.required],
    monto: [0, [Validators.required, Validators.min(0.01)]],
    detalle: ['', Validators.maxLength(255)],
  });
  movimientos: CajaChicaFila[] = [];
  sedes: SelectOption[] = [];
  turno: Api.TurnoCajaResponseDTO | null = null;
  resumen: Api.ResumenCierreCajaDTO | null = null;
  guardando = false;
  error: string | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: TurnosCajaApiService, private readonly lookup: LookupService, private readonly toast: ToastService, private readonly http: HttpClient) {
    effect(() => {
      const actividad = this.caja.actividad();
      untracked(() => { if (actividad && this.cierre.controls.id.value) this.verResumen(); });
    });
  }
  ngOnInit(): void {
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede'); });
    this.http.get<{ abierto?: boolean; turno?: Api.TurnoCajaResponseDTO | null }>(`${environment.apiUrl}/api/v1/turnos-caja/abierto`).pipe(takeUntil(this.destroy$)).subscribe({
      next: (respuesta) => {
        if (respuesta?.abierto && respuesta.turno?.id) this.recordarTurno(respuesta.turno);
      },
      error: () => {
        const turno = this.caja.turno();
        if (turno?.id) this.recordarTurno(turno);
      },
    });
  }

  private recordarTurno(turno: Api.TurnoCajaResponseDTO): void {
    this.turno = turno;
    this.cierre.controls.id.setValue(turno.id ?? '');
    this.caja.recordar(turno);
    this.cargarCajaChica();
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  abrir(): void {
    if (this.guardando) return;
    this.apertura.markAllAsTouched();
    if (this.apertura.invalid) return;
    const value = this.apertura.getRawValue();
    this.guardando = true;
    this.error = null;
    this.api.PostApiV1TurnosCaja({}, { sedeId: value.sedeId, montoInicial: Number(value.montoInicial), observaciones: value.observaciones.trim() || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (turno) => {
        this.guardando = false;
        this.turno = turno;
        this.cierre.controls.id.setValue(turno.id ?? '');
        this.caja.recordar(turno);
        this.cargarCajaChica();
        this.toast.success('Caja abierta.');
      },
      error: (error: unknown) => { this.guardando = false; this.error = problemText(error); },
    });
  }

  verResumen(): void {
    if (this.guardando) return;
    const id = this.cierre.controls.id.value.trim();
    if (!id) return;
    this.guardando = true;
    this.api.GetApiV1TurnosCajaIdResumen({ id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => { this.guardando = false; this.resumen = data; },
      error: (error: unknown) => { this.guardando = false; this.error = problemText(error); },
    });
  }

  cerrar(): void {
    if (this.guardando) return;
    this.cierre.markAllAsTouched();
    if (this.cierre.invalid) return;
    const value = this.cierre.getRawValue();
    this.guardando = true;
    this.api.PostApiV1TurnosCajaIdCierre({ id: value.id }, { montoFinalDeclarado: Number(value.montoFinalDeclarado), observaciones: value.observaciones.trim() || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.guardando = false;
        this.resumen = data;
        this.turno = null;
        this.cierre.controls.id.setValue('');
        this.caja.recordar(null);
        this.movimientos = [];
        this.error = null;
        this.toast.success('Caja cerrada.');
      },
      error: (error: unknown) => { this.guardando = false; this.error = problemText(error); },
    });
  }

  cargarCajaChica(): void {
    const id = this.turno?.id;
    if (!id) { this.movimientos = []; return; }
    this.http.get<CajaChicaFila[]>(`${environment.apiUrl}/api/v1/turnos-caja/${id}/caja-chica`).pipe(takeUntil(this.destroy$)).subscribe({
      next: (filas) => { this.movimientos = filas ?? []; },
      error: () => { this.movimientos = []; },
    });
  }

  registrarCajaChica(): void {
    if (this.guardando) return;
    this.cajaChica.markAllAsTouched();
    const id = this.turno?.id;
    if (!id || this.cajaChica.invalid) return;
    const value = this.cajaChica.getRawValue();
    this.guardando = true;
    this.http.post(`${environment.apiUrl}/api/v1/turnos-caja/${id}/caja-chica`, {
      tipo: value.tipo,
      concepto: value.concepto,
      monto: Number(value.monto),
      detalle: value.detalle.trim() || null,
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.guardando = false;
        this.cajaChica.reset({ tipo: 'EGRESO', concepto: 'TRANSPORTE', monto: 0, detalle: '' });
        this.toast.success('Movimiento de caja chica registrado.');
        this.cargarCajaChica();
        this.verResumen();
      },
      error: (error: unknown) => { this.guardando = false; this.error = problemText(error); },
    });
  }
}

interface CajaChicaFila {
  id?: string;
  tipo?: string;
  concepto?: string;
  monto?: number;
  detalle?: string;
  createdAt?: string;
}

export const TURNOS_CAJA_ROUTES: Routes = [
  { path: '', component: TurnosCajaPageComponent, title: 'Caja', data: { section: 'Ventas', breadcrumb: 'Caja' } },
];
