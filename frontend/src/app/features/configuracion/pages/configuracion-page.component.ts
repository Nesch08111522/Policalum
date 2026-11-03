import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Routes } from '@angular/router';
import { Subject, firstValueFrom, takeUntil } from 'rxjs';
import { environment } from '../../../../environments/environment';
import * as Api from '../../../core/api/openapi.types';
import { RoleGuard } from '../../../guards/role.guard';
import { DataTableComponent } from '../../../shared/components/data-table/data-table.component';
import { FormFieldComponent } from '../../../shared/components/form-field/form-field.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { SelectComponent } from '../../../shared/components/select/select.component';
import { TabsComponent } from '../../../shared/components/tabs/tabs.component';
import { ColumnDef, SelectOption, TabItem } from '../../../shared/models/ui.models';
import { LookupService } from '../../../shared/services/lookup.service';
import { MarcaService } from '../../../shared/services/marca.service';
import { ToastService } from '../../../shared/services/toast.service';
import { applyServerErrors, fieldError, problemText } from '../../../shared/utils/feedback';
import { ConfiguracionApiService } from '../data-access/configuracion.api';
import { FormatoCotizacionComponent } from './formato-cotizacion.component';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-configuracion-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent, TabsComponent, FormFieldComponent, SelectComponent, DataTableComponent, FormatoCotizacionComponent],
  templateUrl: './configuracion-page.component.html',
  styleUrls: ['./configuracion-page.component.css'],
})
export class ConfiguracionPageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly tabs: TabItem[] = [
    { id: 'empresa', label: 'Empresa' },
    { id: 'parametros', label: 'Parámetros' },
    { id: 'series', label: 'Series' },
    { id: 'cotizacion', label: 'Cotización' },
  ];
  tab = 'empresa';
  readonly marcaForm = this.fb.nonNullable.group({
    nombreComercial: ['', [Validators.required, Validators.maxLength(150)]],
  });
  archivoLogo: File | null = null;
  vistaLogo = '/POLICALUM.png';
  guardandoMarca = false;
  readonly empresa = this.fb.nonNullable.group({
    razonSocial: ['', [Validators.required, Validators.maxLength(200)]],
    nombreComercial: [''],
    ruc: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
    direccionFiscal: ['', Validators.required],
    ubigeo: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    email: ['', Validators.email],
    telefono: [''],
  });
  readonly parametros = this.fb.nonNullable.group({
    mesesHistoricosProyeccion: [6, [Validators.required, Validators.min(1)]],
    mesesProyeccionDefault: [3, [Validators.required, Validators.min(1)]],
    mesesProyeccionMax: [12, [Validators.required, Validators.min(1)]],
    minCaracteresJustificacion: [10, [Validators.required, Validators.min(0)]],
    umbralJustificacionAjuste: [0, [Validators.required, Validators.min(0)]],
    maxFilasExportacion: [5000, [Validators.required, Validators.min(1)]],
    preformaMinutosDefault: [30, [Validators.required, Validators.min(1)]],
    preformaMinutosMin: [5, [Validators.required, Validators.min(1)]],
    preformaMinutosMax: [120, [Validators.required, Validators.min(1)]],
    vigenciaCotizacionDias: [7, [Validators.required, Validators.min(1)]],
  });
  readonly serie = this.fb.nonNullable.group({
    sedeId: ['', Validators.required],
    serie: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9]{4}$/)]],
    tipoComprobante: ['BOLETA' as Api.SerieComprobanteRequestDTO['tipoComprobante'], Validators.required],
    activa: [true],
  });
  readonly columnas: ColumnDef[] = [
    { key: 'serie', label: 'Serie' },
    { key: 'tipoComprobante', label: 'Comprobante', kind: 'badge' },
    { key: 'sedeNombre', label: 'Sede' },
    { key: 'activa', label: 'Activa', kind: 'bool' },
  ];
  series: Api.SerieComprobanteResponseDTO[] = [];
  sedes: SelectOption[] = [];
  cargando = false;
  guardando = false;
  pushActivo = false;
  pushTrabajando = false;
  private readonly http = inject(HttpClient);
  error: string | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly api: ConfiguracionApiService, private readonly lookup: LookupService, private readonly toast: ToastService, readonly marca: MarcaService) {}

  ngOnInit(): void {
    this.pushActivo = typeof Notification !== 'undefined' && Notification.permission === 'granted' && !!localStorage.getItem('policalum-push');
    this.lookup.loadSedes().pipe(takeUntil(this.destroy$)).subscribe((rows) => (this.sedes = this.lookup.options(rows, (row) => (row as Api.SedeResponseDTO).nombre ?? 'Sede')));
    this.cargar();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
  cambiarTab(id: string): void { this.tab = id; this.error = null; }

  cargar(): void {
    this.cargando = true;
    this.api.GetApiV1ConfiguracionEmpresa().pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.empresa.patchValue({ razonSocial: data.razonSocial ?? '', nombreComercial: data.nombreComercial ?? '', ruc: data.ruc ?? '', direccionFiscal: data.direccionFiscal ?? '', ubigeo: data.ubigeo ?? '', email: data.email ?? '', telefono: data.telefono ?? '' });
        this.marcaForm.patchValue({ nombreComercial: data.nombreComercial?.trim() || data.razonSocial?.trim() || 'POLICALUM' });
        this.marca.aplicar(data.nombreComercial, data.logoUrl);
        this.vistaLogo = this.marca.logoSrc();
        this.cargando = false;
      },
      error: (error: unknown) => { this.cargando = false; this.error = problemText(error); },
    });
    this.api.GetApiV1ConfiguracionParametros().pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => this.parametros.patchValue({
        mesesHistoricosProyeccion: data.finanzas?.mesesHistoricosProyeccion ?? 6,
        mesesProyeccionDefault: data.finanzas?.mesesProyeccionDefault ?? 3,
        mesesProyeccionMax: data.finanzas?.mesesProyeccionMax ?? 12,
        minCaracteresJustificacion: data.inventario?.minCaracteresJustificacion ?? 10,
        umbralJustificacionAjuste: data.inventario?.umbralJustificacionAjuste ?? 0,
        maxFilasExportacion: data.reportes?.maxFilasExportacion ?? 5000,
        preformaMinutosDefault: data.ventas?.preformaMinutosDefault ?? 30,
        preformaMinutosMin: data.ventas?.preformaMinutosMin ?? 5,
        preformaMinutosMax: data.ventas?.preformaMinutosMax ?? 120,
        vigenciaCotizacionDias: data.ventas?.vigenciaCotizacionDias ?? 7,
      }),
    });
    this.api.GetApiV1ConfiguracionSeries().pipe(takeUntil(this.destroy$)).subscribe({ next: (rows) => (this.series = rows ?? []), error: () => (this.series = []) });
  }

  elegirLogo(event: Event): void {
    const archivo = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.archivoLogo = archivo;
    if (!archivo) return;
    if (archivo.size > 2 * 1024 * 1024) {
      this.archivoLogo = null;
      this.toast.error('La imagen no puede superar 2 MB.');
      return;
    }
    const lector = new FileReader();
    lector.onload = () => { this.vistaLogo = String(lector.result); };
    lector.readAsDataURL(archivo);
  }

  guardarMarca(): void {
    this.marcaForm.markAllAsTouched();
    if (this.marcaForm.invalid || this.guardandoMarca) return;
    const nombre = this.marcaForm.controls.nombreComercial.value.trim();
    this.guardandoMarca = true;
    const publicar = (logoUrl: string | null) => this.marca.guardar(nombre, logoUrl).pipe(takeUntil(this.destroy$)).subscribe({
      next: (data) => {
        this.guardandoMarca = false;
        this.archivoLogo = null;
        this.marca.aplicar(data.nombreComercial, data.logoUrl);
        this.vistaLogo = this.marca.logoSrc();
        this.empresa.controls.nombreComercial.setValue(data.nombreComercial ?? '');
        this.toast.success('Marca actualizada.');
      },
      error: (error: unknown) => { this.guardandoMarca = false; this.toast.error(problemText(error)); },
    });
    if (!this.archivoLogo) {
      publicar(null);
      return;
    }
    this.marca.subirLogo(this.archivoLogo).pipe(takeUntil(this.destroy$)).subscribe({
      next: (respuesta) => publicar(respuesta.ruta),
      error: (error: unknown) => { this.guardandoMarca = false; this.toast.error(problemText(error)); },
    });
  }

  guardarEmpresa(): void {
    this.empresa.markAllAsTouched();
    if (this.empresa.invalid) return;
    const value = this.empresa.getRawValue();
    const body: Api.EmpresaRequestDTO = {
      razonSocial: value.razonSocial.trim(), ruc: value.ruc.trim(), direccionFiscal: value.direccionFiscal.trim(), ubigeo: value.ubigeo.trim(),
      nombreComercial: value.nombreComercial.trim() || undefined, email: value.email.trim() || undefined, telefono: value.telefono.trim() || undefined,
    };
    this.guardando = true;
    this.api.PutApiV1ConfiguracionEmpresa({}, body).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.toast.success('Empresa actualizada.'); },
      error: (error: unknown) => { this.guardando = false; applyServerErrors(this.empresa, error); this.toast.error(problemText(error)); },
    });
  }

  guardarParametros(): void {
    this.parametros.markAllAsTouched();
    if (this.parametros.invalid) return;
    const value = this.parametros.getRawValue();
    const body: Api.ParametrosRequestDTO = {
      finanzas: { mesesHistoricosProyeccion: Number(value.mesesHistoricosProyeccion), mesesProyeccionDefault: Number(value.mesesProyeccionDefault), mesesProyeccionMax: Number(value.mesesProyeccionMax) },
      inventario: { minCaracteresJustificacion: Number(value.minCaracteresJustificacion), umbralJustificacionAjuste: Number(value.umbralJustificacionAjuste) },
      reportes: { maxFilasExportacion: Number(value.maxFilasExportacion) },
      ventas: { preformaMinutosDefault: Number(value.preformaMinutosDefault), preformaMinutosMin: Number(value.preformaMinutosMin), preformaMinutosMax: Number(value.preformaMinutosMax), vigenciaCotizacionDias: Number(value.vigenciaCotizacionDias) },
    };
    this.guardando = true;
    this.api.PutApiV1ConfiguracionParametros({}, body).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.guardando = false; this.toast.success('Parámetros actualizados.'); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  agregarSerie(): void {
    this.serie.markAllAsTouched();
    if (this.serie.invalid) return;
    const value = this.serie.getRawValue();
    const sede = this.sedes.find((item) => item.value === value.sedeId);
    this.series = [...this.series, { serie: value.serie.toUpperCase(), tipoComprobante: value.tipoComprobante, sedeId: value.sedeId, sedeNombre: sede?.label, activa: value.activa }];
    this.serie.reset({ sedeId: '', serie: '', tipoComprobante: 'BOLETA', activa: true });
  }

  quitarSerie(row: unknown): void {
    const item = row as Api.SerieComprobanteResponseDTO;
    this.series = this.series.filter((serie) => !(serie.serie === item.serie && serie.tipoComprobante === item.tipoComprobante && serie.sedeId === item.sedeId));
  }

  guardarSeries(): void {
    const body: Api.SeriesRequestDTO = {
      series: this.series.filter((serie) => serie.sedeId && serie.serie && serie.tipoComprobante).map((serie) => ({
        sedeId: serie.sedeId ?? '',
        serie: serie.serie ?? '',
        tipoComprobante: serie.tipoComprobante ?? 'BOLETA',
        activa: serie.activa ?? true,
      })),
    };
    this.guardando = true;
    this.api.PutApiV1ConfiguracionSeries({}, body).pipe(takeUntil(this.destroy$)).subscribe({
      next: (rows) => { this.guardando = false; this.series = rows ?? []; this.toast.success('Series actualizadas.'); },
      error: (error: unknown) => { this.guardando = false; this.toast.error(problemText(error)); },
    });
  }

  errorEmpresa(nombre: 'razonSocial' | 'ruc' | 'direccionFiscal' | 'ubigeo' | 'email'): string | null {
    return fieldError(this.empresa.controls[nombre], { pattern: nombre === 'ruc' ? 'El RUC tiene 11 dígitos.' : 'El ubigeo tiene 6 dígitos.' });
  }

  errorSerie(): string | null { return fieldError(this.serie.controls.serie, { pattern: 'La serie tiene 4 letras o números.' }); }

  async alternarPush(): Promise<void> {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      this.toast.error('Este navegador no puede recibir notificaciones.');
      return;
    }
    this.pushTrabajando = true;
    try {
      if (this.pushActivo) {
        const registro = await navigator.serviceWorker.getRegistration();
        const actual = await registro?.pushManager.getSubscription();
        const endpoint = actual?.endpoint || localStorage.getItem('policalum-push') || '';
        await actual?.unsubscribe();
        if (endpoint) await firstValueFrom(this.http.delete(`${environment.apiUrl}/api/v1/notificaciones/push`, { params: { endpoint }, responseType: 'text' }));
        localStorage.removeItem('policalum-push');
        this.pushActivo = false;
        this.toast.success('Este dispositivo ya no recibe notificaciones.');
        return;
      }
      const permiso = await Notification.requestPermission();
      if (permiso !== 'granted') {
        this.toast.error('El navegador no dio permiso para las notificaciones.');
        return;
      }
      const registro = await navigator.serviceWorker.register('/sw.js');
      const clave = await firstValueFrom(this.http.get<{ publica: string }>(`${environment.apiUrl}/api/v1/notificaciones/push/clave`));
      const suscripcion = await registro.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: clavePush(clave.publica) });
      const json = suscripcion.toJSON();
      await firstValueFrom(this.http.post(`${environment.apiUrl}/api/v1/notificaciones/push`, {
        endpoint: suscripcion.endpoint,
        keys: { p256dh: json.keys?.['p256dh'], auth: json.keys?.['auth'] },
        dispositivo: navigator.userAgent,
      }, { responseType: 'text' }));
      localStorage.setItem('policalum-push', suscripcion.endpoint);
      this.pushActivo = true;
      this.toast.success('Este dispositivo recibirá las notificaciones.');
    } catch {
      this.toast.error('No se pudo cambiar las notificaciones de este dispositivo.');
    } finally {
      this.pushTrabajando = false;
    }
  }
}

function clavePush(base64: string): ArrayBuffer {
  const limpia = base64.replace(/-/g, '+').replace(/_/g, '/');
  const crudo = atob(limpia);
  const bytes = new Uint8Array(crudo.length);
  for (let i = 0; i < crudo.length; i += 1) bytes[i] = crudo.charCodeAt(i);
  return bytes.buffer as ArrayBuffer;
}

export const CONFIGURACION_ROUTES: Routes = [{
  path: '',
  component: ConfiguracionPageComponent,
  canActivate: [RoleGuard],
  title: 'Configuración',
  data: { expectedRoles: ['ROLE_ADMIN'], section: 'Configuración', breadcrumb: 'Parámetros' },
}];
