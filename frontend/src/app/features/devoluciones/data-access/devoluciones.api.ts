import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class DevolucionesApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Devoluciones(params: QueryMap = {}): Observable<Api.PageDevolucionListadoDTO> {
    return this.api.execute<Api.PageDevolucionListadoDTO>(ENDPOINT_BY_ID['GET_api_v1_devoluciones'], params, params);
  }

  PostApiV1Devoluciones(params: QueryMap = {}, body: Api.DevolucionRequestDTO): Observable<Api.DevolucionResponseDTO> {
    return this.api.execute<Api.DevolucionResponseDTO>(ENDPOINT_BY_ID['POST_api_v1_devoluciones'], params, params, body);
  }

  GetApiV1DevolucionesDashboard(params: QueryMap = {}): Observable<Api.DevolucionDashboardDTO> {
    return this.api.execute<Api.DevolucionDashboardDTO>(ENDPOINT_BY_ID['GET_api_v1_devoluciones_dashboard'], params, params);
  }

  GetApiV1DevolucionesId(params: QueryMap = {}): Observable<Api.DevolucionResponseDTO> {
    return this.api.execute<Api.DevolucionResponseDTO>(ENDPOINT_BY_ID['GET_api_v1_devoluciones_id'], params, params);
  }

  PostApiV1DevolucionesIdAprobar(params: QueryMap = {}, body: Api.AprobarDevolucionRequestDTO): Observable<Api.DevolucionResponseDTO> {
    return this.api.execute<Api.DevolucionResponseDTO>(ENDPOINT_BY_ID['POST_api_v1_devoluciones_id_aprobar'], params, params, body);
  }

  PostApiV1DevolucionesIdRechazar(params: QueryMap = {}, body: Api.RechazarDevolucionRequestDTO): Observable<Api.DevolucionResponseDTO> {
    return this.api.execute<Api.DevolucionResponseDTO>(ENDPOINT_BY_ID['POST_api_v1_devoluciones_id_rechazar'], params, params, body);
  }
}
