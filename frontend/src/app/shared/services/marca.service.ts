import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ConfiguracionPublicaDTO } from '../../core/api/openapi.types';

export const APP_VERSION = '1.0.0';

/** Nombre y logo de la empresa, compartidos por login, menú, pie y comprobantes. */
@Injectable({ providedIn: 'root' })
export class MarcaService {
  private readonly http = inject(HttpClient);
  readonly nombre = signal('POLICALUM');
  readonly logoUrl = signal<string | null>(null);
  private readonly logoRota = signal(false);

  cargar(): void {
    this.http.get<ConfiguracionPublicaDTO>(`${environment.apiUrl}/api/v1/configuracion/publica`).subscribe({
      next: (data) => this.aplicar(data.nombreComercial, data.logoUrl),
      error: () => undefined,
    });
  }

  aplicar(nombre: string | null | undefined, logo: string | null | undefined): void {
    const limpio = nombre?.trim();
    this.nombre.set(limpio || 'POLICALUM');
    this.logoUrl.set(logo?.trim() || null);
    this.logoRota.set(false);
  }

  logoSrc(): string {
    if (this.logoRota()) return '/POLICALUM.png';
    const logo = this.logoUrl();
    if (!logo) return '/POLICALUM.png';
    if (/^https?:\/\//i.test(logo)) return logo;
    return `${environment.apiUrl}/api/v1/configuracion/logo`;
  }

  usarRespaldo(): void {
    this.logoRota.set(true);
  }

  subirLogo(archivo: File): Observable<{ ruta: string }> {
    const cuerpo = new FormData();
    cuerpo.append('archivo', archivo);
    return this.http.post<{ ruta: string }>(`${environment.apiUrl}/api/v1/configuracion/logo`, cuerpo);
  }

  guardar(nombreComercial: string, logoUrl: string | null): Observable<ConfiguracionPublicaDTO> {
    return this.http.put<ConfiguracionPublicaDTO>(`${environment.apiUrl}/api/v1/configuracion/marca`, {
      nombreComercial,
      logoUrl,
    });
  }
}
