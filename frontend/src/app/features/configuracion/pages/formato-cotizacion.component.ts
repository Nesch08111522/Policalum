import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { ToastService } from '../../../shared/services/toast.service';
import { problemText } from '../../../shared/utils/feedback';
import { TokenService } from '../../../services/token.service';

interface Distrito {
  distrito: string;
  precioBase: number;
  unidadesIncluidas: number;
  precioUnidadExtra: number;
  activo: boolean;
}

interface Concepto {
  nombre: string;
  tipo: 'MONTO' | 'PORCENTAJE';
  valor: number;
  activo: boolean;
}

interface Formato {
  utilidadPorcentaje: number;
  distritos: Distrito[];
  conceptos: Concepto[];
}

@Component({
  selector: 'app-formato-cotizacion',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormFieldComponent],
  templateUrl: './formato-cotizacion.component.html',
})
export class FormatoCotizacionComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly destroy$ = new Subject<void>();
  readonly esAdmin = inject(TokenService).isAdmin();
  guardando = false;
  error: string | null = null;
  readonly form = this.fb.nonNullable.group({
    utilidadPorcentaje: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
    distritos: this.fb.array<ReturnType<FormatoCotizacionComponent['distrito']>>([]),
    conceptos: this.fb.array<ReturnType<FormatoCotizacionComponent['concepto']>>([]),
  });

  ngOnInit(): void { this.cargar(); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  get distritos(): FormArray { return this.form.controls.distritos; }
  get conceptos(): FormArray { return this.form.controls.conceptos; }

  cargar(): void {
    this.http.get<Formato>(`${environment.apiUrl}/api/v1/configuracion/cotizacion`).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.form.controls.utilidadPorcentaje.setValue(Number(data.utilidadPorcentaje ?? 0));
        this.distritos.clear();
        for (const item of data.distritos ?? []) this.distritos.push(this.distrito(item));
        this.conceptos.clear();
        for (const item of data.conceptos ?? []) this.conceptos.push(this.concepto(item));
        this.error = null;
      },
      error: (error: unknown) => { this.error = problemText(error); },
    });
  }

  agregarConcepto(): void { this.conceptos.push(this.concepto()); }
  quitarConcepto(index: number): void { this.conceptos.removeAt(index); }

  guardar(): void {
    if (!this.esAdmin || this.guardando) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    this.guardando = true;
    this.http.put<Formato>(`${environment.apiUrl}/api/v1/configuracion/cotizacion`, {
      utilidadPorcentaje: Number(value.utilidadPorcentaje),
      distritos: value.distritos.map((item) => ({
        distrito: item.distrito.trim(),
        precioBase: Number(item.precioBase),
        unidadesIncluidas: Number(item.unidadesIncluidas),
        precioUnidadExtra: Number(item.precioUnidadExtra),
        activo: item.activo,
      })),
      conceptos: value.conceptos.map((item) => ({
        nombre: item.nombre.trim(),
        tipo: item.tipo,
        valor: Number(item.valor),
        activo: item.activo,
      })),
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.toast.success('El formato de cotización quedó actualizado.'); this.cargar(); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  private distrito(item?: Distrito) {
    return this.fb.nonNullable.group({
      distrito: [item?.distrito ?? '', [Validators.required, Validators.maxLength(80)]],
      precioBase: [Number(item?.precioBase ?? 0), [Validators.required, Validators.min(0)]],
      unidadesIncluidas: [Number(item?.unidadesIncluidas ?? 0), [Validators.required, Validators.min(0)]],
      precioUnidadExtra: [Number(item?.precioUnidadExtra ?? 0), [Validators.required, Validators.min(0)]],
      activo: [item?.activo !== false],
    });
  }

  private concepto(item?: Concepto) {
    return this.fb.nonNullable.group({
      nombre: [item?.nombre ?? '', [Validators.required, Validators.maxLength(80)]],
      tipo: [item?.tipo ?? 'MONTO' as Concepto['tipo'], Validators.required],
      valor: [Number(item?.valor ?? 0), [Validators.required, Validators.min(0)]],
      activo: [item?.activo !== false],
    });
  }
}
