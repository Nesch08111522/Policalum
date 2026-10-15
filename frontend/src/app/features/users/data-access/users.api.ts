// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class UsersApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Users(params: QueryMap = {}): Observable<Array<Api.UserResponseDTO>> { return this.api.execute<Array<Api.UserResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_users"], params, params); }
  PostApiV1Users(params: QueryMap = {}, body: Api.UserRequestDTO): Observable<Api.UserResponseDTO> { return this.api.execute<Api.UserResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_users"], params, params, body); }
  DeleteApiV1UsersId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_users_id"], params, params); }
  GetApiV1UsersId(params: QueryMap = {}): Observable<Api.UserResponseDTO> { return this.api.execute<Api.UserResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_users_id"], params, params); }
  PutApiV1UsersId(params: QueryMap = {}, body: Api.UserRequestDTO): Observable<Api.UserResponseDTO> { return this.api.execute<Api.UserResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_users_id"], params, params, body); }
}
