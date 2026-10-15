// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class ProveedoresApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Proveedores(params: QueryMap = {}): Observable<Api.PageProveedorResponseDTO> { return this.api.execute<Api.PageProveedorResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_proveedores"], params, params); }
  PostApiV1Proveedores(params: QueryMap = {}, body: Api.ProveedorRequestDTO): Observable<Api.ProveedorResponseDTO> { return this.api.execute<Api.ProveedorResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_proveedores"], params, params, body); }
  GetApiV1ProveedoresDocumento(params: QueryMap = {}): Observable<Api.ProveedorResponseDTO> { return this.api.execute<Api.ProveedorResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_proveedores_documento"], params, params); }
  GetApiV1ProveedoresEstadisticasDashboard(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_proveedores_estadisticas_dashboard"], params, params); }
  GetApiV1ProveedoresEstadisticasTipoDocumento(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_proveedores_estadisticas_tipo_documento"], params, params); }
  GetApiV1ProveedoresIncompletos(params: QueryMap = {}): Observable<Array<Api.ProveedorResponseDTO>> { return this.api.execute<Array<Api.ProveedorResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_proveedores_incompletos"], params, params); }
  GetApiV1ProveedoresValidarDocumento(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_proveedores_validar_documento"], params, params); }
  DeleteApiV1ProveedoresId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_proveedores_id"], params, params); }
  GetApiV1ProveedoresId(params: QueryMap = {}): Observable<Api.ProveedorResponseDTO> { return this.api.execute<Api.ProveedorResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_proveedores_id"], params, params); }
  PutApiV1ProveedoresId(params: QueryMap = {}, body: Api.ProveedorRequestDTO): Observable<Api.ProveedorResponseDTO> { return this.api.execute<Api.ProveedorResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_proveedores_id"], params, params, body); }
}
