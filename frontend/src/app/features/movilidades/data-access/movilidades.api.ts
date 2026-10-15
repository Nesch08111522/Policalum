// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class MovilidadesApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Movilidades(params: QueryMap = {}): Observable<Api.PageMovilidadListadoDTO> { return this.api.execute<Api.PageMovilidadListadoDTO>(ENDPOINT_BY_ID["GET_api_v1_movilidades"], params, params); }
  PostApiV1Movilidades(params: QueryMap = {}, body: Api.MovilidadRequestDTO): Observable<Api.MovilidadResponseDTO> { return this.api.execute<Api.MovilidadResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_movilidades"], params, params, body); }
  GetApiV1MovilidadesDashboard(params: QueryMap = {}): Observable<Api.MovilidadDashboardDTO> { return this.api.execute<Api.MovilidadDashboardDTO>(ENDPOINT_BY_ID["GET_api_v1_movilidades_dashboard"], params, params); }
  GetApiV1MovilidadesReportesUso(params: QueryMap = {}): Observable<Api.ReporteUsoMovilidadDTO> { return this.api.execute<Api.ReporteUsoMovilidadDTO>(ENDPOINT_BY_ID["GET_api_v1_movilidades_reportes_uso"], params, params); }
  DeleteApiV1MovilidadesId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_movilidades_id"], params, params); }
  GetApiV1MovilidadesId(params: QueryMap = {}): Observable<Api.MovilidadResponseDTO> { return this.api.execute<Api.MovilidadResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_movilidades_id"], params, params); }
  PutApiV1MovilidadesId(params: QueryMap = {}, body: Api.MovilidadRequestDTO): Observable<Api.MovilidadResponseDTO> { return this.api.execute<Api.MovilidadResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_movilidades_id"], params, params, body); }
  PostApiV1MovilidadesIdAsignarConductor(params: QueryMap = {}, body: Api.AsignarConductorRequestDTO): Observable<Api.MovilidadResponseDTO> { return this.api.execute<Api.MovilidadResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_movilidades_id_asignar_conductor"], params, params, body); }
  GetApiV1MovilidadesIdHistorial(params: QueryMap = {}): Observable<Api.PageOrdenDespachoResponseDTO> { return this.api.execute<Api.PageOrdenDespachoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_movilidades_id_historial"], params, params); }
}
