import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { environment } from '../../../environments/environment';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { ToastService } from '../../shared/services/toast.service';
import { problemText } from '../../shared/utils/feedback';

interface CmsPublico {
  primaryColor: string;
  secondaryColor: string;
  baseFontSize: number;
  themeMode: string;
  aviso: string;
  avisoActivo: boolean;
}

@Component({
  selector: 'app-gestor-web-page',
  standalone: true,
  imports: [ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="stack">
      <app-page-header title="Gestor web" subtitle="El sitio público toma estos colores, el aviso y las ofertas desde aquí." />
      <form class="card card-body stack" [formGroup]="forma" (ngSubmit)="guardar()">
        <label>Color primario
          <input type="color" formControlName="primaryColor" />
        </label>
        <label>Color secundario
          <input type="color" formControlName="secondaryColor" />
        </label>
        <label>Tamaño base {{ forma.controls.baseFontSize.value }} px
          <input type="range" min="14" max="22" formControlName="baseFontSize" />
        </label>
        <label>Modo
          <select formControlName="themeMode">
            <option value="aether">Aether oscuro</option>
            <option value="claro">Claro</option>
          </select>
        </label>
        <label>Aviso en la portada
          <input type="text" maxlength="180" formControlName="aviso" placeholder="Cerrado por feriado, atención este domingo..." />
        </label>
        <label class="check"><input type="checkbox" formControlName="avisoActivo" /> Mostrar el aviso</label>
        <div class="preview" [style.background]="forma.controls.primaryColor.value">
          <span [style.color]="forma.controls.secondaryColor.value">Vista del acento</span>
        </div>
        <div class="actions">
          <button type="submit" class="btn btn-primary" [disabled]="guardando">Guardar sitio</button>
          <button type="button" class="btn btn-outline" (click)="sincronizar()" [disabled]="sincronizando">Marcar ofertas de 90 días</button>
        </div>
      </form>
    </section>
  `,
  styles: `
    .stack { display: grid; gap: 16px; width: min(100%, 720px); }
    .card-body { display: grid; gap: 12px; padding: 16px; }
    label { display: grid; gap: 6px; font-size: clamp(0.85rem, 0.8rem + 0.2vw, 1rem); }
    input[type="text"], select { min-height: 40px; }
    .check { grid-auto-flow: column; justify-content: start; align-items: center; }
    .preview { border-radius: 8px; min-height: 72px; display: grid; place-items: center; font-weight: 600; }
    .actions { display: flex; flex-wrap: wrap; gap: 8px; }
  `,
})
export class GestorWebPageComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  guardando = false;
  sincronizando = false;
  readonly forma = this.fb.nonNullable.group({
    primaryColor: ['#1FA2D6', Validators.required],
    secondaryColor: ['#FFFFFF', Validators.required],
    baseFontSize: [16, [Validators.required, Validators.min(14), Validators.max(22)]],
    themeMode: ['aether', Validators.required],
    aviso: ['', Validators.maxLength(180)],
    avisoActivo: [false],
  });

  ngOnInit(): void {
    this.http.get<CmsPublico>(`${environment.apiUrl}/api/v1/cms/settings`).subscribe({
      next: (paquete) => {
        this.forma.patchValue(paquete);
        this.pintar(paquete);
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  guardar(): void {
    if (this.forma.invalid) return;
    this.guardando = true;
    const valor = this.forma.getRawValue();
    this.http.put<CmsPublico>(`${environment.apiUrl}/api/v1/cms/settings`, {
      primaryColor: valor.primaryColor,
      secondaryColor: valor.secondaryColor,
      baseFontSize: Number(valor.baseFontSize),
      themeMode: valor.themeMode,
    }).subscribe({
      next: (paquete) => {
        this.http.put(`${environment.apiUrl}/api/v1/cms/horario`, {
          mensaje: valor.aviso,
          activo: valor.avisoActivo && valor.aviso.trim().length > 0,
        }).subscribe({
          next: () => {
            this.guardando = false;
            this.pintar(paquete);
            this.toast.success('El sitio ya usa esta configuración.');
          },
          error: (error: unknown) => {
            this.guardando = false;
            this.toast.error(problemText(error));
          },
        });
      },
      error: (error: unknown) => {
        this.guardando = false;
        this.toast.error(problemText(error));
      },
    });
  }

  sincronizar(): void {
    this.sincronizando = true;
    this.http.post<{ marcados: number }>(`${environment.apiUrl}/api/v1/cms/ofertas/sincronizar`, {}).subscribe({
      next: (respuesta) => {
        this.sincronizando = false;
        this.toast.success(`Ofertas actualizadas. Productos sin rotación: ${respuesta.marcados}.`);
      },
      error: (error: unknown) => {
        this.sincronizando = false;
        this.toast.error(problemText(error));
      },
    });
  }

  private pintar(paquete: CmsPublico): void {
    document.documentElement.style.setProperty('--cms-primary', paquete.primaryColor);
    document.documentElement.style.setProperty('--cms-secondary', paquete.secondaryColor);
  }
}

export const GESTOR_WEB_ROUTES: Routes = [
  { path: '', component: GestorWebPageComponent, title: 'Gestor web', data: { section: 'Configuración', breadcrumb: 'Gestor web' } },
];
