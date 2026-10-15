// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class VentasApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1Ventas(params: QueryMap = {}): Observable<Api.PageVentaListadoResponseDTO> { return this.api.execute<Api.PageVentaListadoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_ventas"], params, params); }
  PostApiV1Ventas(params: QueryMap = {}, body: Api.RegistrarVentaRequestDTO): Observable<Api.VentaProcesadaResponseDTO> { return this.api.execute<Api.VentaProcesadaResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_ventas"], params, params, body); }
  GetApiV1VentasId(params: QueryMap = {}): Observable<Api.VentaDetalleResponseDTO> { return this.api.execute<Api.VentaDetalleResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_ventas_id"], params, params); }
  PostApiV1VentasIdAnulacion(params: QueryMap = {}, body: Api.AnularVentaRequestDTO): Observable<Api.VentaProcesadaResponseDTO> { return this.api.execute<Api.VentaProcesadaResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_ventas_id_anulacion"], params, params, body); }
}
