import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { OrdenDespachoResponseDTO } from '../../../core/api/openapi.types';

/** La foto de entrega viaja como archivo. El cliente generado no arma el multipart. */
@Injectable({ providedIn: 'root' })
export class DespachoEvidenciaApi {
  private readonly base = environment.apiUrl.replace(/\/+$/, '');

  constructor(private readonly http: HttpClient) {}

  registrar(id: string, archivo: File, recibidoPorNombre: string, recibidoPorDocumento: string): Observable<OrdenDespachoResponseDTO> {
    const cuerpo = new FormData();
    cuerpo.append('archivo', archivo);
    cuerpo.append('recibidoPorNombre', recibidoPorNombre);
    if (recibidoPorDocumento.trim()) cuerpo.append('recibidoPorDocumento', recibidoPorDocumento.trim());
    return this.http.post<OrdenDespachoResponseDTO>(`${this.base}/api/v1/despachos/${id}/evidencia`, cuerpo);
  }

  ver(id: string): Observable<Blob> {
    return this.http.get(`${this.base}/api/v1/despachos/${id}/evidencia`, { responseType: 'blob' });
  }
}
