// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class CategoriasApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Categorias(params: QueryMap = {}): Observable<Api.PageCategoriaResponseDTO> { return this.api.execute<Api.PageCategoriaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_categorias"], params, params); }
  PostApiV1Categorias(params: QueryMap = {}, body: Api.CategoriaRequestDTO): Observable<Api.CategoriaResponseDTO> { return this.api.execute<Api.CategoriaResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_categorias"], params, params, body); }
  GetApiV1CategoriasEstadisticasDashboard(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_categorias_estadisticas_dashboard"], params, params); }
  GetApiV1CategoriasRaices(params: QueryMap = {}): Observable<Array<Api.CategoriaResponseDTO>> { return this.api.execute<Array<Api.CategoriaResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_categorias_raices"], params, params); }
  GetApiV1CategoriasValidarSlug(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_categorias_validar_slug"], params, params); }
  DeleteApiV1CategoriasId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_categorias_id"], params, params); }
  GetApiV1CategoriasId(params: QueryMap = {}): Observable<Api.CategoriaResponseDTO> { return this.api.execute<Api.CategoriaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_categorias_id"], params, params); }
  PutApiV1CategoriasId(params: QueryMap = {}, body: Api.CategoriaRequestDTO): Observable<Api.CategoriaResponseDTO> { return this.api.execute<Api.CategoriaResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_categorias_id"], params, params, body); }
  GetApiV1CategoriasParentIdHijas(params: QueryMap = {}): Observable<Array<Api.CategoriaResponseDTO>> { return this.api.execute<Array<Api.CategoriaResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_categorias_parentId_hijas"], params, params); }
}
