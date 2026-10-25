import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Notificacion {
  id: string;
  tipo: string;
  titulo: string;
  detalle: string;
  ruta: string;
  leida: boolean;
  eliminada: boolean;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificacionesApi {
  private readonly base = `${environment.apiUrl}/api/v1/notificaciones`;
  constructor(private readonly http: HttpClient) {}
  listar(): Observable<Notificacion[]> { return this.http.get<Notificacion[]>(this.base); }
  leida(id: string): Observable<Notificacion> { return this.http.post<Notificacion>(`${this.base}/${id}/leida`, {}); }
  leerTodas(): Observable<{ marcadas: number }> { return this.http.post<{ marcadas: number }>(`${this.base}/leer-todas`, {}); }
  archivar(id: string): Observable<Notificacion> { return this.http.delete<Notificacion>(`${this.base}/${id}`); }
}
