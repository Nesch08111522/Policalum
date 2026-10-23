import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class ReembolsosApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Reembolsos(params: QueryMap = {}): Observable<Api.PageReembolsoResponseDTO> {
    return this.api.execute<Api.PageReembolsoResponseDTO>(ENDPOINT_BY_ID['GET_api_v1_reembolsos'], params, params);
  }

  PostApiV1Reembolsos(params: QueryMap = {}, body: Api.ReembolsoRequestDTO): Observable<Api.ReembolsoResponseDTO> {
    return this.api.execute<Api.ReembolsoResponseDTO>(ENDPOINT_BY_ID['POST_api_v1_reembolsos'], params, params, body);
  }

  GetApiV1ReembolsosId(params: QueryMap = {}): Observable<Api.ReembolsoResponseDTO> {
    return this.api.execute<Api.ReembolsoResponseDTO>(ENDPOINT_BY_ID['GET_api_v1_reembolsos_id'], params, params);
  }

  PostApiV1ReembolsosIdProcesar(params: QueryMap = {}, body: Api.ProcesarReembolsoRequestDTO): Observable<Api.ReembolsoResponseDTO> {
    return this.api.execute<Api.ReembolsoResponseDTO>(ENDPOINT_BY_ID['POST_api_v1_reembolsos_id_procesar'], params, params, body);
  }
}
