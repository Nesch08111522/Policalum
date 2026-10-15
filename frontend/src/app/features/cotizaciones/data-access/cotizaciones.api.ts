// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class CotizacionesApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Cotizaciones(params: QueryMap = {}): Observable<Api.PageCotizacionListadoDTO> { return this.api.execute<Api.PageCotizacionListadoDTO>(ENDPOINT_BY_ID["GET_api_v1_cotizaciones"], params, params); }
  PostApiV1Cotizaciones(params: QueryMap = {}, body: Api.CotizacionRequestDTO): Observable<Api.CotizacionResponseDTO> { return this.api.execute<Api.CotizacionResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_cotizaciones"], params, params, body); }
  GetApiV1CotizacionesDashboard(params: QueryMap = {}): Observable<Record<string, unknown>> { return this.api.execute<Record<string, unknown>>(ENDPOINT_BY_ID["GET_api_v1_cotizaciones_dashboard"], params, params); }
  GetApiV1CotizacionesVencidas(params: QueryMap = {}): Observable<Array<Api.CotizacionListadoDTO>> { return this.api.execute<Array<Api.CotizacionListadoDTO>>(ENDPOINT_BY_ID["GET_api_v1_cotizaciones_vencidas"], params, params); }
  DeleteApiV1CotizacionesId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_cotizaciones_id"], params, params); }
  GetApiV1CotizacionesId(params: QueryMap = {}): Observable<Api.CotizacionResponseDTO> { return this.api.execute<Api.CotizacionResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_cotizaciones_id"], params, params); }
  PutApiV1CotizacionesId(params: QueryMap = {}, body: Api.CotizacionRequestDTO): Observable<Api.CotizacionResponseDTO> { return this.api.execute<Api.CotizacionResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_cotizaciones_id"], params, params, body); }
  PostApiV1CotizacionesIdAceptar(params: QueryMap = {}): Observable<Api.CotizacionResponseDTO> { return this.api.execute<Api.CotizacionResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_cotizaciones_id_aceptar"], params, params); }
  PostApiV1CotizacionesIdConvertir(params: QueryMap = {}, body: Api.ConvertirCotizacionRequestDTO): Observable<Api.VentaProcesadaResponseDTO> { return this.api.execute<Api.VentaProcesadaResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_cotizaciones_id_convertir"], params, params, body); }
  PostApiV1CotizacionesIdRechazar(params: QueryMap = {}, body: Api.RechazarCotizacionRequestDTO): Observable<Api.CotizacionResponseDTO> { return this.api.execute<Api.CotizacionResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_cotizaciones_id_rechazar"], params, params, body); }
}
