// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class CatalogoFraccionesApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1CatalogoFracciones(params: QueryMap = {}): Observable<Api.PageCatalogoFraccionResponseDTO> { return this.api.execute<Api.PageCatalogoFraccionResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_catalogo_fracciones"], params, params); }
  PostApiV1CatalogoFracciones(params: QueryMap = {}, body: Api.CatalogoFraccionRequestDTO): Observable<Api.CatalogoFraccionResponseDTO> { return this.api.execute<Api.CatalogoFraccionResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_catalogo_fracciones"], params, params, body); }
  GetApiV1CatalogoFraccionesEstadisticasDashboard(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_catalogo_fracciones_estadisticas_dashboard"], params, params); }
  GetApiV1CatalogoFraccionesProductoProductoIdDisponibles(params: QueryMap = {}): Observable<Array<Api.CatalogoFraccionResponseDTO>> { return this.api.execute<Array<Api.CatalogoFraccionResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_catalogo_fracciones_producto_productoId_disponibles"], params, params); }
  GetApiV1CatalogoFraccionesValidarFactor(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_catalogo_fracciones_validar_factor"], params, params); }
  DeleteApiV1CatalogoFraccionesId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_catalogo_fracciones_id"], params, params); }
  GetApiV1CatalogoFraccionesId(params: QueryMap = {}): Observable<Api.CatalogoFraccionResponseDTO> { return this.api.execute<Api.CatalogoFraccionResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_catalogo_fracciones_id"], params, params); }
  PutApiV1CatalogoFraccionesId(params: QueryMap = {}, body: Api.CatalogoFraccionRequestDTO): Observable<Api.CatalogoFraccionResponseDTO> { return this.api.execute<Api.CatalogoFraccionResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_catalogo_fracciones_id"], params, params, body); }
}
