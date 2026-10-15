// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class ConfiguracionApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1ConfiguracionEmpresa(params: QueryMap = {}): Observable<Api.EmpresaResponseDTO> { return this.api.execute<Api.EmpresaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_configuracion_empresa"], params, params); }
  PutApiV1ConfiguracionEmpresa(params: QueryMap = {}, body: Api.EmpresaRequestDTO): Observable<Api.EmpresaResponseDTO> { return this.api.execute<Api.EmpresaResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_configuracion_empresa"], params, params, body); }
  GetApiV1ConfiguracionParametros(params: QueryMap = {}): Observable<Api.ParametrosResponseDTO> { return this.api.execute<Api.ParametrosResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_configuracion_parametros"], params, params); }
  PutApiV1ConfiguracionParametros(params: QueryMap = {}, body: Api.ParametrosRequestDTO): Observable<Api.ParametrosResponseDTO> { return this.api.execute<Api.ParametrosResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_configuracion_parametros"], params, params, body); }
  GetApiV1ConfiguracionPublica(params: QueryMap = {}): Observable<Api.ConfiguracionPublicaDTO> { return this.api.execute<Api.ConfiguracionPublicaDTO>(ENDPOINT_BY_ID["GET_api_v1_configuracion_publica"], params, params); }
  GetApiV1ConfiguracionSeries(params: QueryMap = {}): Observable<Array<Api.SerieComprobanteResponseDTO>> { return this.api.execute<Array<Api.SerieComprobanteResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_configuracion_series"], params, params); }
  PutApiV1ConfiguracionSeries(params: QueryMap = {}, body: Api.SeriesRequestDTO): Observable<Array<Api.SerieComprobanteResponseDTO>> { return this.api.execute<Array<Api.SerieComprobanteResponseDTO>>(ENDPOINT_BY_ID["PUT_api_v1_configuracion_series"], params, params, body); }
}
