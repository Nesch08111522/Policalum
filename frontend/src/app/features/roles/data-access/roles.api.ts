// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class RolesApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Roles(params: QueryMap = {}): Observable<Array<Api.RolResponseDTO>> { return this.api.execute<Array<Api.RolResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_roles"], params, params); }
  PostApiV1Roles(params: QueryMap = {}, body: Api.RolRequestDTO): Observable<Api.RolResponseDTO> { return this.api.execute<Api.RolResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_roles"], params, params, body); }
  DeleteApiV1RolesId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_roles_id"], params, params); }
  GetApiV1RolesId(params: QueryMap = {}): Observable<Api.RolResponseDTO> { return this.api.execute<Api.RolResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_roles_id"], params, params); }
  PutApiV1RolesId(params: QueryMap = {}, body: Api.RolRequestDTO): Observable<Api.RolResponseDTO> { return this.api.execute<Api.RolResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_roles_id"], params, params, body); }
}
