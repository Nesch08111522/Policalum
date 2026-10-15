// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class KardexApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Kardex(params: QueryMap = {}): Observable<Api.PageKardexMovimientoResponseDTO> { return this.api.execute<Api.PageKardexMovimientoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_kardex"], params, params); }
  GetApiV1KardexExportar(params: QueryMap = {}): Observable<string> { return this.api.execute<string>(ENDPOINT_BY_ID["GET_api_v1_kardex_exportar"], params, params); }
  GetApiV1KardexMermas(params: QueryMap = {}): Observable<Api.ReporteMermasDTO> { return this.api.execute<Api.ReporteMermasDTO>(ENDPOINT_BY_ID["GET_api_v1_kardex_mermas"], params, params); }
  GetApiV1KardexMovimientos(params: QueryMap = {}): Observable<Api.PageKardexMovimientoDetalleDTO> { return this.api.execute<Api.PageKardexMovimientoDetalleDTO>(ENDPOINT_BY_ID["GET_api_v1_kardex_movimientos"], params, params); }
  PostApiV1KardexMovimientos(params: QueryMap = {}, body: Api.KardexMovimientoRequestDTO): Observable<Api.KardexMovimientoResponseDTO> { return this.api.execute<Api.KardexMovimientoResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_kardex_movimientos"], params, params, body); }
  GetApiV1KardexMovimientosId(params: QueryMap = {}): Observable<Api.KardexMovimientoDetalleDTO> { return this.api.execute<Api.KardexMovimientoDetalleDTO>(ENDPOINT_BY_ID["GET_api_v1_kardex_movimientos_id"], params, params); }
  GetApiV1KardexProductoProductoIdHistorial(params: QueryMap = {}): Observable<Api.HistorialProductoDTO> { return this.api.execute<Api.HistorialProductoDTO>(ENDPOINT_BY_ID["GET_api_v1_kardex_producto_productoId_historial"], params, params); }
  GetApiV1KardexSaldos(params: QueryMap = {}): Observable<Api.PageSaldoKardexDTO> { return this.api.execute<Api.PageSaldoKardexDTO>(ENDPOINT_BY_ID["GET_api_v1_kardex_saldos"], params, params); }
  GetApiV1KardexSaldosAlmacenAlmacenId(params: QueryMap = {}): Observable<Api.PageSaldoKardexDTO> { return this.api.execute<Api.PageSaldoKardexDTO>(ENDPOINT_BY_ID["GET_api_v1_kardex_saldos_almacen_almacenId"], params, params); }
  GetApiV1KardexSaldosProductoProductoId(params: QueryMap = {}): Observable<Api.SaldosProductoDTO> { return this.api.execute<Api.SaldosProductoDTO>(ENDPOINT_BY_ID["GET_api_v1_kardex_saldos_producto_productoId"], params, params); }
  PostApiV1KardexTransferencias(params: QueryMap = {}, body: Api.TransferenciaRequestDTO): Observable<Array<Api.KardexMovimientoResponseDTO>> { return this.api.execute<Array<Api.KardexMovimientoResponseDTO>>(ENDPOINT_BY_ID["POST_api_v1_kardex_transferencias"], params, params, body); }
  GetApiV1KardexValorizacion(params: QueryMap = {}): Observable<Api.ValorizacionKardexDTO> { return this.api.execute<Api.ValorizacionKardexDTO>(ENDPOINT_BY_ID["GET_api_v1_kardex_valorizacion"], params, params); }
  GetApiV1KardexValorizacionAlmacenAlmacenId(params: QueryMap = {}): Observable<Api.ValorizacionKardexDTO> { return this.api.execute<Api.ValorizacionKardexDTO>(ENDPOINT_BY_ID["GET_api_v1_kardex_valorizacion_almacen_almacenId"], params, params); }
}
