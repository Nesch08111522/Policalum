// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class AlmacenesApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Almacenes(params: QueryMap = {}): Observable<Api.PageAlmacenResponseDTO> { return this.api.execute<Api.PageAlmacenResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_almacenes"], params, params); }
  PostApiV1Almacenes(params: QueryMap = {}, body: Api.AlmacenRequestDTO): Observable<Api.AlmacenResponseDTO> { return this.api.execute<Api.AlmacenResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_almacenes"], params, params, body); }
  GetApiV1AlmacenesEstadisticasDashboard(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_almacenes_estadisticas_dashboard"], params, params); }
  GetApiV1AlmacenesSedeSedeId(params: QueryMap = {}): Observable<Array<Api.AlmacenResponseDTO>> { return this.api.execute<Array<Api.AlmacenResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_almacenes_sede_sedeId"], params, params); }
  GetApiV1AlmacenesValidarNombre(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_almacenes_validar_nombre"], params, params); }
  DeleteApiV1AlmacenesId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_almacenes_id"], params, params); }
  GetApiV1AlmacenesId(params: QueryMap = {}): Observable<Api.AlmacenResponseDTO> { return this.api.execute<Api.AlmacenResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_almacenes_id"], params, params); }
  PutApiV1AlmacenesId(params: QueryMap = {}, body: Api.AlmacenRequestDTO): Observable<Api.AlmacenResponseDTO> { return this.api.execute<Api.AlmacenResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_almacenes_id"], params, params, body); }
}
