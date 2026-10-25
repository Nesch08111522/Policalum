import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import * as Api from '../../../core/api/openapi.types';
import { environment } from '../../../../environments/environment';

export interface SedeMapa {
  id?: string;
  nombre?: string;
  ciudad?: string;
  direccion?: string;
  latitud?: number | null;
  longitud?: number | null;
  imagen?: string | null;
  ubicada?: boolean;
}

@Injectable({ providedIn: 'root' })
export class SedeUbicacionApi {
  private readonly base = environment.apiUrl.replace(/\/+$/, '');

  constructor(private readonly http: HttpClient) {}

  mapa(): Observable<SedeMapa[]> {
    return this.http.get<SedeMapa[]>(`${this.base}/api/v1/sedes/mapa`);
  }

  subirImagen(id: string, archivo: File): Observable<Api.SedeResponseDTO> {
    const cuerpo = new FormData();
    cuerpo.append('archivo', archivo);
    return this.http.post<Api.SedeResponseDTO>(`${this.base}/api/v1/sedes/${id}/imagen`, cuerpo);
  }

  foto(ruta: string): Observable<Blob> {
    return this.http.get(`${this.base}/api/v1/sedes/imagenes`, { params: { ruta }, responseType: 'blob' });
  }
}
