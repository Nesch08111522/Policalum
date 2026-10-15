// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class ServiciosApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Servicios(params: QueryMap = {}): Observable<Api.PageServicioResponseDTO> { return this.api.execute<Api.PageServicioResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_servicios"], params, params); }
  PostApiV1Servicios(params: QueryMap = {}, body: Api.ServicioRequestDTO): Observable<Api.ServicioResponseDTO> { return this.api.execute<Api.ServicioResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_servicios"], params, params, body); }
  GetApiV1ServiciosCheckNombre(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_servicios_check_nombre"], params, params); }
  GetApiV1ServiciosDashboardStatistics(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_servicios_dashboard_statistics"], params, params); }
  DeleteApiV1ServiciosId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_servicios_id"], params, params); }
  GetApiV1ServiciosId(params: QueryMap = {}): Observable<Api.ServicioResponseDTO> { return this.api.execute<Api.ServicioResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_servicios_id"], params, params); }
  PutApiV1ServiciosId(params: QueryMap = {}, body: Api.ServicioRequestDTO): Observable<Api.ServicioResponseDTO> { return this.api.execute<Api.ServicioResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_servicios_id"], params, params, body); }
}
