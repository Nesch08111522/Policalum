// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class ClientesApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Clientes(params: QueryMap = {}): Observable<Api.PageClienteResponseDTO> { return this.api.execute<Api.PageClienteResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_clientes"], params, params); }
  PostApiV1Clientes(params: QueryMap = {}, body: Api.ClienteRequestDTO): Observable<Api.ClienteResponseDTO> { return this.api.execute<Api.ClienteResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_clientes"], params, params, body); }
  GetApiV1ClientesComprasActivos(params: QueryMap = {}): Observable<Array<Api.ClienteResponseDTO>> { return this.api.execute<Array<Api.ClienteResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_clientes_compras_activos"], params, params); }
  GetApiV1ClientesComprasInactivos(params: QueryMap = {}): Observable<Array<Api.ClienteResponseDTO>> { return this.api.execute<Array<Api.ClienteResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_clientes_compras_inactivos"], params, params); }
  GetApiV1ClientesDocumentoNumeroDocumento(params: QueryMap = {}): Observable<Api.ClienteResponseDTO> { return this.api.execute<Api.ClienteResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_clientes_documento_numeroDocumento"], params, params); }
  GetApiV1ClientesEstadisticasCount(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_clientes_estadisticas_count"], params, params); }
  GetApiV1ClientesEstadisticasDashboard(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_clientes_estadisticas_dashboard"], params, params); }
  GetApiV1ClientesEstadisticasHoy(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_clientes_estadisticas_hoy"], params, params); }
  GetApiV1ClientesEstadisticasTipoDocumento(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_clientes_estadisticas_tipo_documento"], params, params); }
  GetApiV1ClientesIncompletos(params: QueryMap = {}): Observable<Array<Api.ClienteResponseDTO>> { return this.api.execute<Array<Api.ClienteResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_clientes_incompletos"], params, params); }
  GetApiV1ClientesRecientes(params: QueryMap = {}): Observable<Array<Api.ClienteResponseDTO>> { return this.api.execute<Array<Api.ClienteResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_clientes_recientes"], params, params); }
  GetApiV1ClientesValidarDocumento(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_clientes_validar_documento"], params, params); }
  GetApiV1ClientesValidarEmail(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_clientes_validar_email"], params, params); }
  DeleteApiV1ClientesId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_clientes_id"], params, params); }
  GetApiV1ClientesId(params: QueryMap = {}): Observable<Api.ClienteResponseDTO> { return this.api.execute<Api.ClienteResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_clientes_id"], params, params); }
  PutApiV1ClientesId(params: QueryMap = {}, body: Api.ClienteRequestDTO): Observable<Api.ClienteResponseDTO> { return this.api.execute<Api.ClienteResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_clientes_id"], params, params, body); }
}
