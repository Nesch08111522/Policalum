import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Observable, of, Subject, switchMap, takeUntil } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { TokenService } from '../../../services/token.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { SelectOption } from '../../../shared/models/ui.models';
import { LookupService } from '../../../shared/services/lookup.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemText } from '../../../shared/utils/feedback';
import { PerfilApiService } from '../data-access/perfil.api';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-perfil-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, FormFieldComponent, SelectComponent, ConfirmDialogComponent],
  templateUrl: './perfil-page.component.html',
  styleUrls: ['./perfil-page.component.css'],
})
export class PerfilPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly cd = inject(ChangeDetectorRef);
  readonly datos = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    apellido: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    celular: ['', [Validators.required, Validators.maxLength(15), Validators.pattern(/^[0-9+() -]{6,15}$/)]],
    documento: ['', Validators.pattern(/^(\d{8}|\d{11})?$/)],
    sedeId: [''],
    biografia: ['', Validators.maxLength(500)],
    sueldoBase: [''],
    diaCobro: [''],
  });
  readonly clave = this.fb.nonNullable.group({
    actual: ['', Validators.required],
    nueva: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_]).*$/)]],
    confirmacion: ['', Validators.required],
  });
  perfil: Api.PerfilResponseDTO | null = null;
  sedes: SelectOption[] = [];
  vistaFoto = '';
  archivoFoto: File | null = null;
  private fotoQuitada = false;
  guardando = false;
  cambiandoClave = false;
  cerrando = false;
  confirmarSesiones = false;
  errorDatos: string | null = null;
  errorClave: string | null = null;
  secreto = '';
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly api: PerfilApiService,
    private readonly lookup: LookupService,
    private readonly tokens: TokenService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit(): void {
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => {
      this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede');
      this.cd.markForCheck();
    });
    this.cargar();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  get rolVisible(): string {
    if (this.perfil?.rol === 'ROLE_ADMIN') return 'Administrador';
    if (this.perfil?.rol === 'ROLE_MULTIFUNCIONAL') return 'Multifuncional';
    return this.perfil?.rol || 'Usuario';
  }

  get iniciales(): string {
    const nombre = `${this.perfil?.nombre ?? ''} ${this.perfil?.apellido ?? ''}`.trim() || 'P';
    return nombre.split(/\s+/).slice(0, 2).map((parte) => parte.charAt(0).toUpperCase()).join('');
  }

  cargar(): void {
    this.api.actual().pipe(takeUntil(this.destroy$)).subscribe({
      next: (perfil) => this.pintar(perfil),
      error: (error: unknown) => { this.errorDatos = problemText(error); this.cd.markForCheck(); },
    });
  }

  guardar(): void {
    if (this.guardando) return;
    this.datos.markAllAsTouched();
    if (this.datos.invalid) return;
    const value = this.datos.getRawValue();
    const body: Api.PerfilActualizarRequestDTO = {
      nombre: value.nombre.trim(),
      apellido: value.apellido.trim(),
      email: value.email.trim(),
      celular: value.celular.trim(),
      documento: value.documento.trim(),
      biografia: value.biografia.trim(),
      sedeId: value.sedeId || undefined,
      foto: this.fotoQuitada && !this.archivoFoto ? '' : undefined,
    };
    if (this.perfil?.puedeEditarLaboral) {
      body.sueldoBase = value.sueldoBase === '' ? undefined : Number(value.sueldoBase);
      body.diaCobro = value.diaCobro === '' ? undefined : Number(value.diaCobro);
    }
    this.guardando = true;
    this.errorDatos = null;
    const subir: Observable<unknown> = this.archivoFoto ? this.api.subirFoto(this.archivoFoto) : of(null);
    subir.pipe(switchMap(() => this.api.actualizar(body)), takeUntil(this.destroy$)).subscribe({
      next: (perfil) => {
        this.guardando = false;
        this.archivoFoto = null;
        this.fotoQuitada = false;
        this.pintar(perfil);
        this.toast.success('Perfil actualizado.');
      },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.datos, error); this.errorDatos = problemText(error); },
    });
  }

  cambiarClave(): void {
    if (this.cambiandoClave) return;
    this.clave.markAllAsTouched();
    if (this.clave.invalid) return;
    const value = this.clave.getRawValue();
    if (value.nueva !== value.confirmacion) {
      this.errorClave = 'La confirmación no coincide con la nueva contraseña.';
      return;
    }
    this.cambiandoClave = true;
    this.errorClave = null;
    this.api.contrasena(value).pipe(takeUntil(this.destroy$)).subscribe({
      next: (perfil) => {
        this.cambiandoClave = false;
        this.clave.reset();
        this.aplicarSesion(perfil);
        this.toast.success('Contraseña actualizada. Las otras sesiones quedaron cerradas.');
      },
      error: (error: unknown) => { this.cambiandoClave = false; applyServerErrors(this.clave, error); this.errorClave = problemText(error); },
    });
  }

  cambiarTotp(event: Event): void {
    const activo = (event.target as HTMLInputElement).checked;
    this.api.totp({ activo }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (perfil) => {
        this.perfil = { ...this.perfil, ...perfil, foto: perfil.foto ?? this.perfil?.foto };
        this.secreto = perfil.totpSecreto ?? '';
        this.aplicarSesion(perfil);
        this.toast.success(activo ? 'Doble factor activado.' : 'Doble factor desactivado.');
      },
      error: (error: unknown) => {
        (event.target as HTMLInputElement).checked = !activo;
        this.toast.error(problemText(error));
      },
    });
  }

  cerrarSesiones(): void {
    this.cerrando = true;
    this.api.cerrarSesiones().pipe(takeUntil(this.destroy$)).subscribe({
      next: (perfil) => {
        this.cerrando = false;
        this.confirmarSesiones = false;
        this.aplicarSesion(perfil);
        this.toast.success('Las otras sesiones fueron cerradas.');
      },
      error: (error: unknown) => { this.cerrando = false; this.toast.error(problemText(error)); },
    });
  }

  elegirFoto(event: Event): void {
    const archivo = (event.target as HTMLInputElement).files?.[0];
    if (!archivo) return;
    const permitida = ['image/jpeg', 'image/png', 'image/webp'].includes(archivo.type);
    if (!permitida || archivo.size > 2 * 1024 * 1024) {
      this.toast.error('La foto debe ser JPG, PNG o WEBP de hasta 2 MB.');
      return;
    }
    this.archivoFoto = archivo;
    this.fotoQuitada = false;
    const lector = new FileReader();
    lector.onload = () => { this.vistaFoto = String(lector.result ?? ''); this.cd.markForCheck(); };
    lector.readAsDataURL(archivo);
  }

  quitarFoto(): void {
    this.vistaFoto = '';
    this.archivoFoto = null;
    this.fotoQuitada = true;
  }

  cancelar(): void {
    if (this.perfil) this.pintar(this.perfil);
  }

  ultimoIngreso(valor?: string): string {
    if (!valor) return 'Sin registro';
    const iso = /[zZ]|[+-]\d{2}:\d{2}$/.test(valor) ? valor : `${valor}Z`;
    const fecha = new Date(iso);
    if (Number.isNaN(fecha.getTime())) return valor;
    return new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Lima' }).format(fecha);
  }

  errorDe(nombre: 'nombre' | 'apellido' | 'email' | 'celular' | 'documento' | 'biografia'): string | null {
    return fieldError(this.datos.controls[nombre], {
      pattern: nombre === 'celular' ? 'Usa entre 6 y 15 dígitos, espacios o guiones.' : 'Usa 8 dígitos para DNI o 11 para RUC.',
    });
  }

  errorClaveDe(nombre: 'actual' | 'nueva' | 'confirmacion'): string | null {
    return fieldError(this.clave.controls[nombre], { pattern: 'Incluye mayúscula, minúscula, número y un símbolo.' });
  }

  private mostrarFoto(foto: string | undefined): void {
    if (!foto) {
      this.vistaFoto = '';
      return;
    }
    if (foto.startsWith('data:image/') || /^https?:\/\//i.test(foto)) {
      this.vistaFoto = foto;
      return;
    }
    this.api.verFoto().pipe(takeUntil(this.destroy$)).subscribe({
      next: (blob) => { this.vistaFoto = URL.createObjectURL(blob); this.cd.markForCheck(); },
      error: () => { this.vistaFoto = ''; this.cd.markForCheck(); },
    });
  }

  private pintar(perfil: Api.PerfilResponseDTO): void {
    this.perfil = perfil;
    this.mostrarFoto(perfil.foto);
    this.datos.reset({
      nombre: perfil.nombre ?? '',
      apellido: perfil.apellido ?? '',
      email: perfil.email ?? '',
      celular: perfil.celular ?? '',
      documento: perfil.documento ?? '',
      sedeId: perfil.sedeId ?? '',
      biografia: perfil.biografia ?? '',
      sueldoBase: perfil.sueldoBase == null ? '' : String(perfil.sueldoBase),
      diaCobro: perfil.diaCobro == null ? '' : String(perfil.diaCobro),
    });
    if (!perfil.puedeEditarLaboral) {
      this.datos.controls.sedeId.disable();
      this.datos.controls.sueldoBase.disable();
      this.datos.controls.diaCobro.disable();
    }
    this.aplicarSesion(perfil);
    this.cd.markForCheck();
  }

  private aplicarSesion(perfil: Api.PerfilResponseDTO): void {
    if (perfil.token) this.tokens.setToken(perfil.token);
    if (perfil.refreshToken) this.tokens.setRefresh(perfil.refreshToken);
    const actual = this.tokens.getUser();
    if (!actual) return;
    this.tokens.setUser({
      ...actual,
      nombre: `${perfil.nombre ?? ''} ${perfil.apellido ?? ''}`.trim() || actual.nombre,
      email: perfil.email ?? actual.email,
      totpConfigurado: perfil.totpEnabled ?? actual.totpConfigurado,
    });
  }
}

export const PERFIL_ROUTES: Routes = [{
  path: '',
  component: PerfilPageComponent,
  title: 'Editar perfil',
  data: { section: 'Mi cuenta', breadcrumb: 'Editar perfil' },
}];
