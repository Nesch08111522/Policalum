// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class ProductosApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Productos(params: QueryMap = {}): Observable<Api.PageProductoResponseDTO> { return this.api.execute<Api.PageProductoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_productos"], params, params); }
  PostApiV1Productos(params: QueryMap = {}, body: Api.ProductoRequestDTO): Observable<Api.ProductoResponseDTO> { return this.api.execute<Api.ProductoResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_productos"], params, params, body); }
  GetApiV1ProductosCheckSku(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_productos_check_sku"], params, params); }
  GetApiV1ProductosDashboardStatistics(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_productos_dashboard_statistics"], params, params); }
  DeleteApiV1ProductosId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_productos_id"], params, params); }
  GetApiV1ProductosId(params: QueryMap = {}): Observable<Api.ProductoResponseDTO> { return this.api.execute<Api.ProductoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_productos_id"], params, params); }
  PutApiV1ProductosId(params: QueryMap = {}, body: Api.ProductoRequestDTO): Observable<Api.ProductoResponseDTO> { return this.api.execute<Api.ProductoResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_productos_id"], params, params, body); }
}
