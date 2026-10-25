import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReportService {
  constructor(private readonly http: HttpClient) {}

  descargar(modulo: string, formato: 'pdf' | 'xlsx', filtros = ''): Observable<Blob> {
    let params = new HttpParams().set('modulo', modulo).set('formato', formato);
    if (filtros) params = params.set('filtros', filtros);
    return this.http.get(`${environment.apiUrl}/api/v1/exportacion`, { params, responseType: 'blob' }).pipe(
      tap((blob) => guardarBlob(blob, `${modulo}.${formato}`)),
    );
  }

  cotizacion(id: string, formato: 'pdf' | 'xlsx'): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/api/v1/cotizaciones/${id}/documento`, {
      params: { formato },
      responseType: 'blob',
    }).pipe(tap((blob) => guardarBlob(blob, `cotizacion-${id.slice(0, 8)}.${formato}`)));
  }
}

export function guardarBlob(blob: Blob, nombre: string): void {
  const url = window.URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  window.URL.revokeObjectURL(url);
}
