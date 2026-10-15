// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class FinanzasApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1FinanzasCuentas(params: QueryMap = {}): Observable<Array<Api.CuentaFinancieraResponseDTO>> { return this.api.execute<Array<Api.CuentaFinancieraResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_finanzas_cuentas"], params, params); }
  PostApiV1FinanzasCuentas(params: QueryMap = {}, body: Api.CuentaFinancieraRequestDTO): Observable<Api.CuentaFinancieraResponseDTO> { return this.api.execute<Api.CuentaFinancieraResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_finanzas_cuentas"], params, params, body); }
  DeleteApiV1FinanzasCuentasId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_finanzas_cuentas_id"], params, params); }
  GetApiV1FinanzasCuentasId(params: QueryMap = {}): Observable<Api.CuentaFinancieraResponseDTO> { return this.api.execute<Api.CuentaFinancieraResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_cuentas_id"], params, params); }
  PutApiV1FinanzasCuentasId(params: QueryMap = {}, body: Api.CuentaFinancieraRequestDTO): Observable<Api.CuentaFinancieraResponseDTO> { return this.api.execute<Api.CuentaFinancieraResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_finanzas_cuentas_id"], params, params, body); }
  GetApiV1FinanzasCuentasIdSaldos(params: QueryMap = {}): Observable<Api.PageSaldoCuentaResponseDTO> { return this.api.execute<Api.PageSaldoCuentaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_cuentas_id_saldos"], params, params); }
  PostApiV1FinanzasCuentasIdSaldos(params: QueryMap = {}, body: Api.RegistrarSaldoCuentaRequestDTO): Observable<Api.SaldoCuentaResponseDTO> { return this.api.execute<Api.SaldoCuentaResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_finanzas_cuentas_id_saldos"], params, params, body); }
  GetApiV1FinanzasDashboardEvolucionMensual(params: QueryMap = {}): Observable<Api.EvolucionMensualResponseDTO> { return this.api.execute<Api.EvolucionMensualResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_dashboard_evolucion_mensual"], params, params); }
  GetApiV1FinanzasDashboardKpis(params: QueryMap = {}): Observable<Api.KpisFinancierosResponseDTO> { return this.api.execute<Api.KpisFinancierosResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_dashboard_kpis"], params, params); }
  GetApiV1FinanzasDashboardPatrimonio(params: QueryMap = {}): Observable<Api.PatrimonioResponseDTO> { return this.api.execute<Api.PatrimonioResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_dashboard_patrimonio"], params, params); }
  GetApiV1FinanzasFlujoCaja(params: QueryMap = {}): Observable<Api.FlujoCajaRealResponseDTO> { return this.api.execute<Api.FlujoCajaRealResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_flujo_caja"], params, params); }
  GetApiV1FinanzasFlujoCajaDevengado(params: QueryMap = {}): Observable<Api.FlujoDevengadoResponseDTO> { return this.api.execute<Api.FlujoDevengadoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_flujo_caja_devengado"], params, params); }
  GetApiV1FinanzasFlujoCajaProyeccion(params: QueryMap = {}): Observable<Api.ProyeccionFlujoResponseDTO> { return this.api.execute<Api.ProyeccionFlujoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_flujo_caja_proyeccion"], params, params); }
  GetApiV1FinanzasRetirosSocios(params: QueryMap = {}): Observable<Api.PageRetiroSocioListadoDTO> { return this.api.execute<Api.PageRetiroSocioListadoDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_retiros_socios"], params, params); }
  PostApiV1FinanzasRetirosSocios(params: QueryMap = {}, body: Api.RetiroSocioRequestDTO): Observable<Api.RetiroSocioResponseDTO> { return this.api.execute<Api.RetiroSocioResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_finanzas_retiros_socios"], params, params, body); }
  GetApiV1FinanzasRetirosSociosDashboard(params: QueryMap = {}): Observable<Api.RetiroSocioDashboardDTO> { return this.api.execute<Api.RetiroSocioDashboardDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_retiros_socios_dashboard"], params, params); }
  GetApiV1FinanzasRetirosSociosId(params: QueryMap = {}): Observable<Api.RetiroSocioResponseDTO> { return this.api.execute<Api.RetiroSocioResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_retiros_socios_id"], params, params); }
  PostApiV1FinanzasRetirosSociosIdAnular(params: QueryMap = {}, body: Api.AnularRetiroRequestDTO): Observable<Api.RetiroSocioResponseDTO> { return this.api.execute<Api.RetiroSocioResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_finanzas_retiros_socios_id_anular"], params, params, body); }
  GetApiV1FinanzasUtilidadNeta(params: QueryMap = {}): Observable<Api.UtilidadNetaResponseDTO> { return this.api.execute<Api.UtilidadNetaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_utilidad_neta"], params, params); }
  GetApiV1FinanzasUtilidadNetaPeriodo(params: QueryMap = {}): Observable<Api.UtilidadNetaResponseDTO> { return this.api.execute<Api.UtilidadNetaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_finanzas_utilidad_neta_periodo"], params, params); }
}
