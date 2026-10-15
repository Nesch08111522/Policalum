// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class DespachosApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Despachos(params: QueryMap = {}): Observable<Api.PageOrdenDespachoResponseDTO> { return this.api.execute<Api.PageOrdenDespachoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_despachos"], params, params); }
  PostApiV1Despachos(params: QueryMap = {}, body: Api.CrearDespachoRequestDTO): Observable<Api.OrdenDespachoResponseDTO> { return this.api.execute<Api.OrdenDespachoResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_despachos"], params, params, body); }
  GetApiV1DespachosDashboard(params: QueryMap = {}): Observable<Api.DespachoDashboardDTO> { return this.api.execute<Api.DespachoDashboardDTO>(ENDPOINT_BY_ID["GET_api_v1_despachos_dashboard"], params, params); }
  GetApiV1DespachosRepartidorUserId(params: QueryMap = {}): Observable<Array<Api.OrdenDespachoResponseDTO>> { return this.api.execute<Array<Api.OrdenDespachoResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_despachos_repartidor_userId"], params, params); }
  GetApiV1DespachosSeguimientoCodigo(params: QueryMap = {}): Observable<Api.OrdenDespachoResponseDTO> { return this.api.execute<Api.OrdenDespachoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_despachos_seguimiento_codigo"], params, params); }
  GetApiV1DespachosId(params: QueryMap = {}): Observable<Api.OrdenDespachoResponseDTO> { return this.api.execute<Api.OrdenDespachoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_despachos_id"], params, params); }
  PostApiV1DespachosIdAsignarRepartidor(params: QueryMap = {}, body: Api.AsignarRepartidorRequestDTO): Observable<Api.OrdenDespachoResponseDTO> { return this.api.execute<Api.OrdenDespachoResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_despachos_id_asignar_repartidor"], params, params, body); }
  PatchApiV1DespachosIdEstado(params: QueryMap = {}, body: Api.CambiarEstadoDespachoRequestDTO): Observable<Api.OrdenDespachoResponseDTO> { return this.api.execute<Api.OrdenDespachoResponseDTO>(ENDPOINT_BY_ID["PATCH_api_v1_despachos_id_estado"], params, params, body); }
  GetApiV1DespachosIdEvidencia(params: QueryMap = {}): Observable<string> { return this.api.execute<string>(ENDPOINT_BY_ID["GET_api_v1_despachos_id_evidencia"], params, params); }
  PostApiV1DespachosIdEvidencia(params: QueryMap = {}): Observable<Api.OrdenDespachoResponseDTO> { return this.api.execute<Api.OrdenDespachoResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_despachos_id_evidencia"], params, params); }
}
