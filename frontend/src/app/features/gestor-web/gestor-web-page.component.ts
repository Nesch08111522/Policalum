import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink, Routes } from '@angular/router';
import { RoleGuard } from '../../guards/role.guard';
import { environment } from '../../../environments/environment';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { ToastService } from '../../shared/services/toast.service';
import { problemText } from '../../shared/utils/feedback';

interface DiaHorario {
  dia: string;
  abierto: boolean;
  desde?: string;
  hasta?: string;
}

interface Atencion {
  configurado: boolean;
  abiertoAhora: boolean;
  texto: string;
}

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
  heroKicker?: string;
  heroTitulo?: string;
  heroTexto?: string;
  atencion?: Atencion;
  dias?: DiaHorario[];
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

const SEMANA = [
  { dia: 'LUNES', etiqueta: 'Lunes', abierto: true, desde: '08:00', hasta: '18:00' },
  { dia: 'MARTES', etiqueta: 'Martes', abierto: true, desde: '08:00', hasta: '18:00' },
  { dia: 'MIERCOLES', etiqueta: 'Miércoles', abierto: true, desde: '08:00', hasta: '18:00' },
  { dia: 'JUEVES', etiqueta: 'Jueves', abierto: true, desde: '08:00', hasta: '18:00' },
  { dia: 'VIERNES', etiqueta: 'Viernes', abierto: true, desde: '08:00', hasta: '18:00' },
  { dia: 'SABADO', etiqueta: 'Sábado', abierto: true, desde: '08:00', hasta: '13:00' },
  { dia: 'DOMINGO', etiqueta: 'Domingo', abierto: false, desde: '08:00', hasta: '13:00' },
];

@Component({
  selector: 'app-gestor-web-page',
  standalone: true,
  imports: [ReactiveFormsModule, PageHeaderComponent, RouterLink],
  template: `
    <section class="stack">
      <app-page-header title="Sitio web" subtitle="Cada bloque es un formulario. El teléfono, el RUC, el correo y el nombre son los de la empresa. Las sedes se editan en Sedes y la cotización en Configuración." />
      <form [formGroup]="forma" (ngSubmit)="guardar()">
        <div class="acordeon">
          <details open>
            <summary>Datos principales</summary>
            <div class="acordeon-cuerpo form-grid">
              <label class="field">Nombre comercial <input class="input" type="text" formControlName="nombreComercial" /></label>
              <label class="field">Razón social <input class="input" type="text" formControlName="razonSocial" /></label>
              <label class="field">RUC <input class="input" type="text" formControlName="ruc" maxlength="11" inputmode="numeric" /></label>
              <label class="field">Teléfono <input class="input" type="text" formControlName="telefono" /></label>
              <label class="field">Correo <input class="input" type="email" formControlName="email" /></label>
              <label class="field">Ubigeo <input class="input" type="text" formControlName="ubigeo" maxlength="6" inputmode="numeric" /></label>
              <label class="field span-2">Dirección <input class="input" type="text" formControlName="direccionFiscal" /></label>
              <p class="nota span-2">Las sedes y su dirección se cambian en <a routerLink="/modules/sedes">Sedes</a>. La utilidad y la movilidad de la cotización se cambian en <a routerLink="/modules/configuracion">Configuración → Cotización</a>.</p>
            </div>
          </details>

          <details>
            <summary>Portada y pie</summary>
            <div class="acordeon-cuerpo form-grid">
              <label class="field">Antetítulo <input class="input" type="text" maxlength="80" formControlName="heroKicker" /></label>
              <label class="field">Título <input class="input" type="text" maxlength="180" formControlName="heroTitulo" /></label>
              <label class="field span-2">Texto de la portada <textarea class="textarea" rows="3" maxlength="400" formControlName="heroTexto"></textarea></label>
              <label class="field span-2">Franja superior <input class="input" type="text" maxlength="180" formControlName="banner" /></label>
              <label class="field span-2">Texto del pie <textarea class="textarea" rows="3" maxlength="500" formControlName="resumen"></textarea></label>
            </div>
          </details>

          <details open>
            <summary>Horario de atención</summary>
            <div class="acordeon-cuerpo">
              <p class="nota">Marca los días que no se trabaja y la hora de cierre. El sitio dice solo si hoy atienden, de qué hora a qué hora, o si ya cerraron.</p>
              @for (dia of dias.controls; track dia.controls.dia.value) {
                <div class="horario-fila">
                  <strong>{{ dia.controls.etiqueta.value }}</strong>
                  <label class="check-row"><input class="checkbox" type="checkbox" [formControl]="dia.controls.abierto" /> Abierto</label>
                  @if (dia.controls.abierto.value) {
                    <input class="input" type="time" [formControl]="dia.controls.desde" />
                    <input class="input" type="time" [formControl]="dia.controls.hasta" />
                  }
                </div>
              }
              <p class="ganancia">{{ vistaHoy }}</p>
            </div>
          </details>

          <details>
            <summary>Redes y contacto</summary>
            <div class="acordeon-cuerpo form-grid">
              <label class="field span-2">Correo de soporte <input class="input" type="email" formControlName="correoSoporte" /></label>
              <label class="field">Facebook <input class="input" type="url" formControlName="facebook" /></label>
              <label class="field">Instagram <input class="input" type="url" formControlName="instagram" /></label>
              <label class="field">LinkedIn <input class="input" type="url" formControlName="linkedin" /></label>
              <label class="field">YouTube <input class="input" type="url" formControlName="youtube" /></label>
            </div>
          </details>

          <details>
            <summary>Textos legales</summary>
            <div class="acordeon-cuerpo">
              <p class="nota">Si los dejas vacíos, el sitio usa el texto explicado de fábrica.</p>
              <label class="field">Privacidad <textarea class="textarea" rows="4" formControlName="textoPrivacidad"></textarea></label>
              <label class="field">Términos <textarea class="textarea" rows="4" formControlName="textoTerminos"></textarea></label>
              <label class="field">Garantía <textarea class="textarea" rows="3" formControlName="textoGarantia"></textarea></label>
              <label class="field">Preguntas frecuentes <textarea class="textarea" rows="4" formControlName="textoFaq"></textarea></label>
            </div>
          </details>

          <details>
            <summary>Aviso, color y vista previa</summary>
            <div class="acordeon-cuerpo form-grid">
              <label class="field span-2">Aviso en la portada <input class="input" type="text" maxlength="180" formControlName="aviso" /></label>
              <label class="check-row span-2"><input class="checkbox" type="checkbox" formControlName="avisoActivo" /> Mostrar el aviso</label>
              <label class="field">Color primario <input class="input-color" type="color" formControlName="primaryColor" /></label>
              <aside class="preview span-2">
                <p class="franja">{{ forma.controls.banner.value || 'Franja del sitio' }}</p>
                <strong>{{ forma.controls.heroTitulo.value || forma.controls.nombreComercial.value || 'POLICALUM' }}</strong>
                <p>{{ forma.controls.heroTexto.value }}</p>
                <p>{{ forma.controls.resumen.value }}</p>
                <p>{{ forma.controls.telefono.value }} · {{ forma.controls.email.value }}</p>
                <p>RUC {{ forma.controls.ruc.value }}</p>
                <p>{{ vistaHoy }}</p>
              </aside>
            </div>
          </details>
        </div>
        <div class="actions">
          <button type="submit" class="btn btn-primary" [disabled]="guardando">Guardar sitio</button>
          <button type="button" class="btn btn-outline" (click)="sincronizar()" [disabled]="sincronizando">Marcar ofertas de 90 días</button>
        </div>
      </form>
    </section>
  `,
  styles: `
    form { display: grid; gap: 12px; }
    .field { display: grid; gap: 6px; color: var(--color-text); font-size: var(--text-sm); font-weight: 600; }
    .nota { margin: 0; color: var(--color-text-muted); font-size: 0.9rem; }
    .input-color { width: 4.5rem; height: 2.25rem; padding: 0; border: 0; background: transparent; }
    .preview { border-radius: 12px; padding: 16px; background: var(--color-bg-subtle); color: var(--color-text); border: 1px solid var(--color-border); display: grid; gap: 6px; }
    .franja { margin: 0; text-align: center; color: var(--color-primary); font-size: 12px; }
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
  readonly dias = this.fb.array(SEMANA.map((dia) => this.fb.nonNullable.group({
    dia: [dia.dia],
    etiqueta: [dia.etiqueta],
    abierto: [dia.abierto],
    desde: [dia.desde],
    hasta: [dia.hasta],
  })));
  readonly forma = this.fb.nonNullable.group({
    nombreComercial: ['', Validators.maxLength(150)],
    razonSocial: ['', Validators.maxLength(200)],
    ruc: ['', Validators.pattern(/^$|^\d{11}$/)],
    telefono: [''],
    email: ['', Validators.email],
    direccionFiscal: [''],
    ubigeo: ['', Validators.pattern(/^$|^\d{6}$/)],
    banner: ['', Validators.maxLength(180)],
    resumen: ['', Validators.maxLength(500)],
    correoSoporte: ['', Validators.email],
    facebook: [''],
    instagram: [''],
    linkedin: [''],
    youtube: [''],
    textoPrivacidad: [''],
    textoTerminos: [''],
    textoGarantia: [''],
    textoFaq: [''],
    heroKicker: ['', Validators.maxLength(80)],
    heroTitulo: ['', Validators.maxLength(180)],
    heroTexto: ['', Validators.maxLength(400)],
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
        for (const dia of paquete.dias ?? []) {
          const fila = this.dias.controls.find((control) => control.controls.dia.value === dia.dia);
          fila?.patchValue({
            abierto: dia.abierto,
            desde: dia.desde || '08:00',
            hasta: dia.hasta || '18:00',
          });
        }
      },
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

  get vistaHoy(): string {
    const ahora = new Date(new Date().toLocaleString('en-US', { timeZone: 'America/Lima' }));
    const clave = ['DOMINGO', 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO'][ahora.getDay()];
    const nombres: Record<string, string> = { LUNES: 'lunes', MARTES: 'martes', MIERCOLES: 'miércoles', JUEVES: 'jueves', VIERNES: 'viernes', SABADO: 'sábado', DOMINGO: 'domingo' };
    const fila = this.dias.getRawValue().find((dia) => dia.dia === clave);
    if (!fila?.abierto) return `Hoy, ${nombres[clave]}, no atendemos.`;
    const rango = `de ${this.hora(fila.desde)} a ${this.hora(fila.hasta)}`;
    const minutos = ahora.getHours() * 60 + ahora.getMinutes();
    const abre = this.minutos(fila.desde);
    const cierra = this.minutos(fila.hasta);
    if (minutos < abre) return this.frase(nombres[clave], 'atendemos', rango, 'Aún no abrimos.');
    if (minutos >= cierra) return this.frase(nombres[clave], 'atendimos', rango, 'Ya cerramos.');
    return this.frase(nombres[clave], 'atendemos', rango, 'Estamos abiertos.');
  }

  private frase(dia: string, verbo: string, rango: string, cierre: string): string {
    return `Hoy, ${dia}, ${verbo} ${rango}. ${cierre}`.replace('p. m..', 'p. m.').replace('a. m..', 'a. m.');
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
      this.guardarCms(valor, 'El sitio quedó guardado. El RUC, la razón social, la dirección y el ubigeo siguen pendientes, así que el registro de la empresa no cambió.');
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
      horarioSemana: this.textoHorario(['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES']),
      horarioSabado: this.textoHorario(['SABADO']),
      facebook: valor.facebook,
      instagram: valor.instagram,
      linkedin: valor.linkedin,
      youtube: valor.youtube,
      correoSoporte: valor.correoSoporte,
      textoPrivacidad: valor.textoPrivacidad,
      textoTerminos: valor.textoTerminos,
      textoGarantia: valor.textoGarantia,
      textoFaq: valor.textoFaq,
      heroKicker: valor.heroKicker,
      heroTitulo: valor.heroTitulo,
      heroTexto: valor.heroTexto,
    }).subscribe({
      next: () => {
        this.http.put(`${environment.apiUrl}/api/v1/cms/horario`, {
          mensaje: valor.aviso,
          activo: valor.avisoActivo && valor.aviso.trim().length > 0,
          dias: this.dias.getRawValue().map((dia) => ({ dia: dia.dia, abierto: dia.abierto, desde: dia.desde, hasta: dia.hasta })),
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

  private textoHorario(claves: string[]): string {
    const partes = this.dias.getRawValue()
      .filter((dia) => claves.includes(dia.dia))
      .map((dia) => dia.abierto ? `${dia.desde} a ${dia.hasta}` : 'Cerrado');
    if (!partes.length) return '';
    return (partes.every((parte) => parte === partes[0]) ? partes[0] : partes.join(' · ')).slice(0, 80);
  }

  private minutos(hora: string): number {
    const [h, m] = hora.split(':').map(Number);
    return h * 60 + m;
  }

  private hora(valor: string): string {
    const [h, m] = valor.split(':').map(Number);
    const sufijo = h < 12 ? 'a. m.' : 'p. m.';
    const doce = h % 12 || 12;
    return `${doce}:${String(m).padStart(2, '0')} ${sufijo}`;
  }
}

export const GESTOR_WEB_ROUTES: Routes = [
  { path: '', component: GestorWebPageComponent, canActivate: [RoleGuard], title: 'Sitio web', data: { section: 'Configuración', breadcrumb: 'Sitio web', expectedRoles: ['ROLE_ADMIN'] } },
];
