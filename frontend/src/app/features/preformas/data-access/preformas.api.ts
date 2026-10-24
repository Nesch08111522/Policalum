import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class PreformasApiService {
  constructor(private readonly api: BackendApiService) {}

  PostApiV1Preformas(params: QueryMap = {}, body: Api.PreformaVentaRequestDTO): Observable<Api.PreformaVentaResponseDTO> {
    return this.api.execute<Api.PreformaVentaResponseDTO>(ENDPOINT_BY_ID['POST_api_v1_preformas'], params, params, body);
  }

  GetApiV1PreformasAbiertas(params: QueryMap = {}): Observable<Array<Api.PreformaListadoDTO>> {
    return this.api.execute<Array<Api.PreformaListadoDTO>>(ENDPOINT_BY_ID['GET_api_v1_preformas_abiertas'], params, params);
  }

  GetApiV1PreformasDashboard(params: QueryMap = {}): Observable<Api.PreformaDashboardDTO> {
    return this.api.execute<Api.PreformaDashboardDTO>(ENDPOINT_BY_ID['GET_api_v1_preformas_dashboard'], params, params);
  }

  GetApiV1PreformasVencidas(params: QueryMap = {}): Observable<Api.PagePreformaListadoDTO> {
    return this.api.execute<Api.PagePreformaListadoDTO>(ENDPOINT_BY_ID['GET_api_v1_preformas_vencidas'], params, params);
  }

  DeleteApiV1PreformasId(params: QueryMap = {}): Observable<unknown> {
    return this.api.execute<unknown>(ENDPOINT_BY_ID['DELETE_api_v1_preformas_id'], params, params);
  }

  GetApiV1PreformasId(params: QueryMap = {}): Observable<Api.PreformaVentaResponseDTO> {
    return this.api.execute<Api.PreformaVentaResponseDTO>(ENDPOINT_BY_ID['GET_api_v1_preformas_id'], params, params);
  }

  PutApiV1PreformasId(params: QueryMap = {}, body: Api.PreformaVentaRequestDTO): Observable<Api.PreformaVentaResponseDTO> {
    return this.api.execute<Api.PreformaVentaResponseDTO>(ENDPOINT_BY_ID['PUT_api_v1_preformas_id'], params, params, body);
  }

  PostApiV1PreformasIdCancelar(params: QueryMap = {}, body: Api.CancelarPreformaRequestDTO): Observable<Api.PreformaVentaResponseDTO> {
    return this.api.execute<Api.PreformaVentaResponseDTO>(ENDPOINT_BY_ID['POST_api_v1_preformas_id_cancelar'], params, params, body);
  }

  PostApiV1PreformasIdConfirmar(params: QueryMap = {}, body: Api.ConfirmarPreformaRequestDTO): Observable<Api.ConfirmacionPreformaDTO> {
    return this.api.execute<Api.ConfirmacionPreformaDTO>(ENDPOINT_BY_ID['POST_api_v1_preformas_id_confirmar'], params, params, body);
  }
}
