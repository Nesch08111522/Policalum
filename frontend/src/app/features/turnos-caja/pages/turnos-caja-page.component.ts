import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { SelectOption } from '../../../shared/models/ui.models';
import { PenPipe } from '../../../shared/pipes/format.pipes';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { problemText } from '../../../shared/utils/feedback';
import { TurnosCajaApiService } from '../data-access/turnos-caja.api';

const TURNO_KEY = 'erp-turno-caja';

@Component({
  selector: 'app-turnos-caja-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FormFieldComponent, SelectComponent, PenPipe],
  templateUrl: './turnos-caja-page.component.html',
  styleUrls: ['./turnos-caja-page.component.css'],
})
export class TurnosCajaPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
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
  sedes: SelectOption[] = [];
  turno: Api.TurnoCajaResponseDTO | null = null;
  resumen: Api.ResumenCierreCajaDTO | null = null;
  guardando = false;
  error: string | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: TurnosCajaApiService, private readonly lookup: LookupService, private readonly toast: ToastService) {}
  ngOnInit(): void {
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => { this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede'); });
    const saved = sessionStorage.getItem(TURNO_KEY);
    if (!saved) return;
    try { this.turno = JSON.parse(saved) as Api.TurnoCajaResponseDTO; this.cierre.controls.id.setValue(this.turno.id ?? ''); } catch { sessionStorage.removeItem(TURNO_KEY); }
  }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  abrir(): void {
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
        sessionStorage.setItem(TURNO_KEY, JSON.stringify(turno));
        this.toast.success('Caja abierta.');
      },
      error: (error: unknown) => { this.guardando = false; this.error = problemText(error); },
    });
  }

  verResumen(): void {
    const id = this.cierre.controls.id.value.trim();
    if (!id) return;
    this.guardando = true;
    this.api.GetApiV1TurnosCajaIdResumen({ id }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => { this.guardando = false; this.resumen = data; },
      error: (error: unknown) => { this.guardando = false; this.error = problemText(error); },
    });
  }

  cerrar(): void {
    this.cierre.markAllAsTouched();
    if (this.cierre.invalid) return;
    const value = this.cierre.getRawValue();
    this.guardando = true;
    this.api.PostApiV1TurnosCajaIdCierre({ id: value.id }, { montoFinalDeclarado: Number(value.montoFinalDeclarado), observaciones: value.observaciones.trim() || undefined }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.guardando = false;
        this.resumen = data;
        sessionStorage.removeItem(TURNO_KEY);
        this.toast.success('Caja cerrada.');
      },
      error: (error: unknown) => { this.guardando = false; this.error = problemText(error); },
    });
  }
}

export const TURNOS_CAJA_ROUTES: Routes = [
  { path: '', component: TurnosCajaPageComponent, title: 'Caja', data: { section: 'Ventas', breadcrumb: 'Caja' } },
];
