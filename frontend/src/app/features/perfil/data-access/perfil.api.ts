import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import * as Api from '../../../core/api/openapi.types';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PerfilApiService {
  private readonly base = environment.apiUrl.replace(/\/+$/, '');

  constructor(private readonly api: BackendApiService, private readonly http: HttpClient) {}

  subirFoto(archivo: File): Observable<Api.PerfilResponseDTO> {
    const cuerpo = new FormData();
    cuerpo.append('archivo', archivo);
    return this.http.post<Api.PerfilResponseDTO>(`${this.base}/api/v1/perfil/foto`, cuerpo);
  }

  verFoto(): Observable<Blob> {
    return this.http.get(`${this.base}/api/v1/perfil/foto`, { responseType: 'blob' });
  }

  actual(params: QueryMap = {}): Observable<Api.PerfilResponseDTO> {
    return this.api.execute<Api.PerfilResponseDTO>(ENDPOINT_BY_ID['GET_api_v1_perfil'], params, params);
  }

  actualizar(body: Api.PerfilActualizarRequestDTO): Observable<Api.PerfilResponseDTO> {
    return this.api.execute<Api.PerfilResponseDTO>(ENDPOINT_BY_ID['PUT_api_v1_perfil'], {}, {}, body);
  }

  contrasena(body: Api.PerfilContrasenaRequestDTO): Observable<Api.PerfilResponseDTO> {
    return this.api.execute<Api.PerfilResponseDTO>(ENDPOINT_BY_ID['POST_api_v1_perfil_contrasena'], {}, {}, body);
  }

  totp(body: Api.PerfilTotpRequestDTO): Observable<Api.PerfilResponseDTO> {
    return this.api.execute<Api.PerfilResponseDTO>(ENDPOINT_BY_ID['POST_api_v1_perfil_totp'], {}, {}, body);
  }

  cerrarSesiones(): Observable<Api.PerfilResponseDTO> {
    return this.api.execute<Api.PerfilResponseDTO>(ENDPOINT_BY_ID['POST_api_v1_perfil_cerrar_sesiones']);
  }
}
