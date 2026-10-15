// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class TurnosCajaApiService {
  constructor(private readonly api: BackendApiService) {}

  PostApiV1TurnosCaja(params: QueryMap = {}, body: Api.AperturaCajaRequestDTO): Observable<Api.TurnoCajaResponseDTO> { return this.api.execute<Api.TurnoCajaResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_turnos_caja"], params, params, body); }
  PostApiV1TurnosCajaIdCierre(params: QueryMap = {}, body: Api.CierreCajaRequestDTO): Observable<Api.ResumenCierreCajaDTO> { return this.api.execute<Api.ResumenCierreCajaDTO>(ENDPOINT_BY_ID["POST_api_v1_turnos_caja_id_cierre"], params, params, body); }
  GetApiV1TurnosCajaIdResumen(params: QueryMap = {}): Observable<Api.ResumenCierreCajaDTO> { return this.api.execute<Api.ResumenCierreCajaDTO>(ENDPOINT_BY_ID["GET_api_v1_turnos_caja_id_resumen"], params, params); }
}
