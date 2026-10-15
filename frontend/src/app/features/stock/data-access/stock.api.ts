// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class StockApiService {
  constructor(private readonly api: BackendApiService) {}

  PostApiV1Stock(params: QueryMap = {}, body: Api.StockRequestDTO): Observable<Api.StockResponseDTO> { return this.api.execute<Api.StockResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_stock"], params, params, body); }
  PostApiV1StockAjuste(params: QueryMap = {}, body: Api.AjusteStockRequestDTO): Observable<Api.AjusteStockResponseDTO> { return this.api.execute<Api.AjusteStockResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_stock_ajuste"], params, params, body); }
  GetApiV1StockAjustes(params: QueryMap = {}): Observable<Api.PageAjusteStockResponseDTO> { return this.api.execute<Api.PageAjusteStockResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_stock_ajustes"], params, params); }
  GetApiV1StockAlmacenAlmacenId(params: QueryMap = {}): Observable<Api.PageStockResponseDTO> { return this.api.execute<Api.PageStockResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_stock_almacen_almacenId"], params, params); }
  GetApiV1StockAlmacenAlmacenIdProductoProductoId(params: QueryMap = {}): Observable<Api.StockResponseDTO> { return this.api.execute<Api.StockResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_stock_almacen_almacenId_producto_productoId"], params, params); }
  GetApiV1StockBajoStock(params: QueryMap = {}): Observable<Api.PageSaldoKardexDTO> { return this.api.execute<Api.PageSaldoKardexDTO>(ENDPOINT_BY_ID["GET_api_v1_stock_bajo_stock"], params, params); }
  GetApiV1StockConsolidado(params: QueryMap = {}): Observable<Api.PageStockConsolidadoDTO> { return this.api.execute<Api.PageStockConsolidadoDTO>(ENDPOINT_BY_ID["GET_api_v1_stock_consolidado"], params, params); }
  GetApiV1StockDashboard(params: QueryMap = {}): Observable<Api.StockDashboardDTO> { return this.api.execute<Api.StockDashboardDTO>(ENDPOINT_BY_ID["GET_api_v1_stock_dashboard"], params, params); }
  GetApiV1StockProductoProductoId(params: QueryMap = {}): Observable<Array<Api.StockResponseDTO>> { return this.api.execute<Array<Api.StockResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_stock_producto_productoId"], params, params); }
  PostApiV1StockTransferencia(params: QueryMap = {}, body: Api.TransferenciaStockRequestDTO): Observable<Api.TransferenciaStockResponseDTO> { return this.api.execute<Api.TransferenciaStockResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_stock_transferencia"], params, params, body); }
  GetApiV1StockTransferencias(params: QueryMap = {}): Observable<Api.PageTransferenciaStockResponseDTO> { return this.api.execute<Api.PageTransferenciaStockResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_stock_transferencias"], params, params); }
  GetApiV1StockTransferenciasId(params: QueryMap = {}): Observable<Api.TransferenciaStockResponseDTO> { return this.api.execute<Api.TransferenciaStockResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_stock_transferencias_id"], params, params); }
}
