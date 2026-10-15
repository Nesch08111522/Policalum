// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class CuentasPorPagarApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1CuentasPorPagar(params: QueryMap = {}): Observable<Api.PageCuentaPorPagarResponseDTO> { return this.api.execute<Api.PageCuentaPorPagarResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_cuentas_por_pagar"], params, params); }
  GetApiV1CuentasPorPagarCompraCompraId(params: QueryMap = {}): Observable<Api.CuentaPorPagarResponseDTO> { return this.api.execute<Api.CuentaPorPagarResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_cuentas_por_pagar_compra_compraId"], params, params); }
  GetApiV1CuentasPorPagarDashboard(params: QueryMap = {}): Observable<Api.ResumenCxPDashboardDTO> { return this.api.execute<Api.ResumenCxPDashboardDTO>(ENDPOINT_BY_ID["GET_api_v1_cuentas_por_pagar_dashboard"], params, params); }
  GetApiV1CuentasPorPagarProximasVencer(params: QueryMap = {}): Observable<Array<Api.CuentaPorPagarResponseDTO>> { return this.api.execute<Array<Api.CuentaPorPagarResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_cuentas_por_pagar_proximas_vencer"], params, params); }
  GetApiV1CuentasPorPagarVencidas(params: QueryMap = {}): Observable<Array<Api.CuentaPorPagarResponseDTO>> { return this.api.execute<Array<Api.CuentaPorPagarResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_cuentas_por_pagar_vencidas"], params, params); }
  GetApiV1CuentasPorPagarId(params: QueryMap = {}): Observable<Api.CuentaPorPagarResponseDTO> { return this.api.execute<Api.CuentaPorPagarResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_cuentas_por_pagar_id"], params, params); }
  PostApiV1CuentasPorPagarIdPago(params: QueryMap = {}, body: Api.PagoCxPRequestDTO): Observable<Api.CuentaPorPagarResponseDTO> { return this.api.execute<Api.CuentaPorPagarResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_cuentas_por_pagar_id_pago"], params, params, body); }
}
