// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class ComprasApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Compras(params: QueryMap = {}): Observable<Api.PageCompraListadoDTO> { return this.api.execute<Api.PageCompraListadoDTO>(ENDPOINT_BY_ID["GET_api_v1_compras"], params, params); }
  PostApiV1Compras(params: QueryMap = {}, body: Api.CompraRequestDTO): Observable<Api.CompraResponseDTO> { return this.api.execute<Api.CompraResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_compras"], params, params, body); }
  GetApiV1ComprasDashboard(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_compras_dashboard"], params, params); }
  DeleteApiV1ComprasId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_compras_id"], params, params); }
  GetApiV1ComprasId(params: QueryMap = {}): Observable<Api.CompraResponseDTO> { return this.api.execute<Api.CompraResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_compras_id"], params, params); }
  PutApiV1ComprasId(params: QueryMap = {}, body: Api.CompraRequestDTO): Observable<Api.CompraResponseDTO> { return this.api.execute<Api.CompraResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_compras_id"], params, params, body); }
  GetApiV1ComprasIdAdjuntos(params: QueryMap = {}): Observable<Array<Api.CompraAdjuntoResponseDTO>> { return this.api.execute<Array<Api.CompraAdjuntoResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_compras_id_adjuntos"], params, params); }
  PostApiV1ComprasIdAdjuntos(params: QueryMap = {}): Observable<Api.CompraAdjuntoResponseDTO> { return this.api.execute<Api.CompraAdjuntoResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_compras_id_adjuntos"], params, params); }
  DeleteApiV1ComprasIdAdjuntosAdjuntoId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_compras_id_adjuntos_adjuntoId"], params, params); }
  GetApiV1ComprasIdAdjuntosAdjuntoId(params: QueryMap = {}): Observable<string> { return this.api.execute<string>(ENDPOINT_BY_ID["GET_api_v1_compras_id_adjuntos_adjuntoId"], params, params); }
  PostApiV1ComprasIdAnulacion(params: QueryMap = {}, body: Api.AnularCompraRequestDTO): Observable<Api.CompraResponseDTO> { return this.api.execute<Api.CompraResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_compras_id_anulacion"], params, params, body); }
  PostApiV1ComprasIdRecepcion(params: QueryMap = {}, body: Api.RecepcionCompraRequestDTO): Observable<Api.CompraResponseDTO> { return this.api.execute<Api.CompraResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_compras_id_recepcion"], params, params, body); }
}
