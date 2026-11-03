import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink, Routes } from '@angular/router';
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
  banner?: string;
  resumen?: string;
  horarioSemana?: string;
  horarioSabado?: string;
  facebook?: string;
  instagram?: string;
  linkedin?: string;
  youtube?: string;
  correoSoporte?: string;
  textoPrivacidad?: string;
  textoTerminos?: string;
  textoGarantia?: string;
  textoFaq?: string;
}

interface Empresa {
  ruc?: string;
  razonSocial?: string;
  nombreComercial?: string;
  direccionFiscal?: string;
  ubigeo?: string;
  telefono?: string;
  email?: string;
  logoUrl?: string;
}

@Component({
  selector: 'app-gestor-web-page',
  standalone: true,
  imports: [ReactiveFormsModule, PageHeaderComponent, RouterLink],
  template: `
    <section class="stack">
      <app-page-header title="Sitio web" subtitle="Lo que guardas aquí es lo que ve el sitio. El teléfono, el RUC, el correo y el nombre son los de la empresa. Las sedes se editan en Sedes y la cotización en Configuración." />
      <form class="card card-body stack" [formGroup]="forma" (ngSubmit)="guardar()">
        <h2>Empresa</h2>
        <label>Nombre comercial <input type="text" formControlName="nombreComercial" /></label>
        <label>Razón social <input type="text" formControlName="razonSocial" /></label>
        <label>RUC <input type="text" formControlName="ruc" maxlength="11" /></label>
        <label>Teléfono <input type="text" formControlName="telefono" /></label>
        <label>Correo <input type="email" formControlName="email" /></label>
        <label>Dirección <input type="text" formControlName="direccionFiscal" /></label>
        <label>Ubigeo <input type="text" formControlName="ubigeo" maxlength="6" /></label>
        <p class="nota">Las sedes y su dirección se cambian en <a routerLink="/modules/sedes">Sedes</a>. La utilidad y la movilidad de la cotización se cambian en <a routerLink="/modules/configuracion">Configuración → Cotización</a>.</p>
        <h2>Portada y pie</h2>
        <label>Franja superior <input type="text" maxlength="180" formControlName="banner" /></label>
        <label>Texto del pie <textarea rows="3" maxlength="500" formControlName="resumen"></textarea></label>
        <label>Horario de lunes a viernes <input type="text" maxlength="80" formControlName="horarioSemana" placeholder="8:00 a. m. – 6:00 p. m." /></label>
        <label>Horario sábado <input type="text" maxlength="80" formControlName="horarioSabado" /></label>
        <label>Correo de soporte <input type="email" formControlName="correoSoporte" /></label>
        <label>Facebook <input type="url" formControlName="facebook" /></label>
        <label>Instagram <input type="url" formControlName="instagram" /></label>
        <label>LinkedIn <input type="url" formControlName="linkedin" /></label>
        <label>YouTube <input type="url" formControlName="youtube" /></label>
        <h2>Textos legales</h2>
        <p class="nota">Si los dejas vacíos, el sitio usa el texto explicado de fábrica.</p>
        <label>Privacidad <textarea rows="4" formControlName="textoPrivacidad"></textarea></label>
        <label>Términos <textarea rows="4" formControlName="textoTerminos"></textarea></label>
        <label>Garantía <textarea rows="3" formControlName="textoGarantia"></textarea></label>
        <label>Preguntas frecuentes <textarea rows="4" formControlName="textoFaq"></textarea></label>
        <h2>Aviso y color</h2>
        <label>Aviso en la portada <input type="text" maxlength="180" formControlName="aviso" /></label>
        <label class="check"><input type="checkbox" formControlName="avisoActivo" /> Mostrar el aviso</label>
        <label>Color primario <input type="color" formControlName="primaryColor" /></label>
        <aside class="preview">
          <p class="franja">{{ forma.controls.banner.value || 'Franja del sitio' }}</p>
          <strong>{{ forma.controls.nombreComercial.value || 'POLICALUM' }}</strong>
          <p>{{ forma.controls.resumen.value }}</p>
          <p>{{ forma.controls.telefono.value }} · {{ forma.controls.email.value }}</p>
          <p>RUC {{ forma.controls.ruc.value }}</p>
        </aside>
        <div class="actions">
          <button type="submit" class="btn btn-primary" [disabled]="guardando">Guardar sitio</button>
          <button type="button" class="btn btn-outline" (click)="sincronizar()" [disabled]="sincronizando">Marcar ofertas de 90 días</button>
        </div>
      </form>
    </section>
  `,
  styles: `
    .stack { display: grid; gap: 16px; width: min(100%, 760px); }
    .card-body { display: grid; gap: 12px; padding: 16px; }
    h2 { margin: 8px 0 0; font-size: 1rem; }
    label { display: grid; gap: 6px; }
    input[type="text"], input[type="email"], input[type="url"], textarea { min-height: 40px; width: 100%; }
    .check { grid-auto-flow: column; justify-content: start; align-items: center; }
    .nota { margin: 0; color: var(--text-muted, #667); font-size: 0.9rem; }
    .preview { border-radius: 12px; padding: 16px; background: #071018; color: #f3f7fb; display: grid; gap: 6px; }
    .franja { margin: 0; text-align: center; color: #3ad0ea; font-size: 12px; }
    .actions { display: flex; flex-wrap: wrap; gap: 8px; }
  `,
})
export class GestorWebPageComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  guardando = false;
  sincronizando = false;
  private logoUrl = '';
  readonly forma = this.fb.nonNullable.group({
    nombreComercial: ['', Validators.maxLength(150)],
    razonSocial: ['', Validators.maxLength(200)],
    ruc: ['', Validators.pattern(/^\d{11}$/)],
    telefono: [''],
    email: ['', Validators.email],
    direccionFiscal: [''],
    ubigeo: ['', Validators.pattern(/^\d{6}$/)],
    banner: ['', Validators.maxLength(180)],
    resumen: ['', Validators.maxLength(500)],
    horarioSemana: ['', Validators.maxLength(80)],
    horarioSabado: ['', Validators.maxLength(80)],
    correoSoporte: ['', Validators.email],
    facebook: [''],
    instagram: [''],
    linkedin: [''],
    youtube: [''],
    textoPrivacidad: [''],
    textoTerminos: [''],
    textoGarantia: [''],
    textoFaq: [''],
    primaryColor: ['#1FA2D6', Validators.required],
    secondaryColor: ['#FFFFFF', Validators.required],
    baseFontSize: [16, [Validators.required, Validators.min(14), Validators.max(22)]],
    themeMode: ['aether', Validators.required],
    aviso: ['', Validators.maxLength(180)],
    avisoActivo: [false],
  });

  ngOnInit(): void {
    this.http.get<CmsPublico>(`${environment.apiUrl}/api/v1/cms/settings`).subscribe({
      next: (paquete) => this.forma.patchValue(paquete),
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
    this.http.get<Empresa>(`${environment.apiUrl}/api/v1/configuracion/empresa`).subscribe({
      next: (empresa) => {
        this.logoUrl = empresa.logoUrl ?? '';
        this.forma.patchValue({
          nombreComercial: empresa.nombreComercial ?? '',
          razonSocial: empresa.razonSocial ?? '',
          ruc: empresa.ruc ?? '',
          telefono: empresa.telefono ?? '',
          email: empresa.email ?? '',
          direccionFiscal: empresa.direccionFiscal ?? '',
          ubigeo: empresa.ubigeo ?? '',
        });
      },
      error: (error: unknown) => this.toast.error(problemText(error)),
    });
  }

  guardar(): void {
    if (this.forma.invalid || this.guardando) {
      this.forma.markAllAsTouched();
      return;
    }
    const valor = this.forma.getRawValue();
    const empresaLista = valor.ruc.length === 11 && valor.razonSocial.trim() && valor.direccionFiscal.trim() && valor.ubigeo.length === 6;
    this.guardando = true;
    if (!empresaLista) {
      this.guardarCms(valor, 'El pie y el banner quedaron guardados. El RUC, la razón social, la dirección y el ubigeo siguen pendientes, así que el registro de la empresa no cambió.');
      return;
    }
    this.http.put(`${environment.apiUrl}/api/v1/configuracion/empresa`, {
      ruc: valor.ruc,
      razonSocial: valor.razonSocial,
      nombreComercial: valor.nombreComercial || undefined,
      direccionFiscal: valor.direccionFiscal,
      ubigeo: valor.ubigeo,
      telefono: valor.telefono || undefined,
      email: valor.email || undefined,
      logoUrl: this.logoUrl || undefined,
    }).subscribe({
      next: () => this.guardarCms(valor),
      error: (error: unknown) => {
        this.guardando = false;
        this.toast.error(problemText(error));
      },
    });
  }

  private guardarCms(valor: ReturnType<GestorWebPageComponent['forma']['getRawValue']>, aviso = 'El ERP, la base y el sitio quedaron con estos datos.'): void {
    this.http.put<CmsPublico>(`${environment.apiUrl}/api/v1/cms/settings`, {
      primaryColor: valor.primaryColor,
      secondaryColor: valor.secondaryColor,
      baseFontSize: Number(valor.baseFontSize),
      themeMode: valor.themeMode,
      banner: valor.banner,
      resumen: valor.resumen,
      horarioSemana: valor.horarioSemana,
      horarioSabado: valor.horarioSabado,
      facebook: valor.facebook,
      instagram: valor.instagram,
      linkedin: valor.linkedin,
      youtube: valor.youtube,
      correoSoporte: valor.correoSoporte,
      textoPrivacidad: valor.textoPrivacidad,
      textoTerminos: valor.textoTerminos,
      textoGarantia: valor.textoGarantia,
      textoFaq: valor.textoFaq,
    }).subscribe({
      next: () => {
        this.http.put(`${environment.apiUrl}/api/v1/cms/horario`, {
          mensaje: valor.aviso,
          activo: valor.avisoActivo && valor.aviso.trim().length > 0,
        }).subscribe({
          next: () => {
            this.guardando = false;
            this.toast.success(aviso);
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
}

export const GESTOR_WEB_ROUTES: Routes = [
  { path: '', component: GestorWebPageComponent, title: 'Sitio web', data: { section: 'Configuración', breadcrumb: 'Sitio web', expectedRoles: ['ROLE_ADMIN'] } },
];
