// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class SedesApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Sedes(params: QueryMap = {}): Observable<Api.PageSedeResponseDTO> { return this.api.execute<Api.PageSedeResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_sedes"], params, params); }
  PostApiV1Sedes(params: QueryMap = {}, body: Api.SedeRequestDTO): Observable<Api.SedeResponseDTO> { return this.api.execute<Api.SedeResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_sedes"], params, params, body); }
  GetApiV1SedesEstado(params: QueryMap = {}): Observable<Api.PageSedeResponseDTO> { return this.api.execute<Api.PageSedeResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_sedes_estado"], params, params); }
  DeleteApiV1SedesId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_sedes_id"], params, params); }
  GetApiV1SedesId(params: QueryMap = {}): Observable<Api.SedeResponseDTO> { return this.api.execute<Api.SedeResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_sedes_id"], params, params); }
  PutApiV1SedesId(params: QueryMap = {}, body: Api.SedeRequestDTO): Observable<Api.SedeResponseDTO> { return this.api.execute<Api.SedeResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_sedes_id"], params, params, body); }
}
