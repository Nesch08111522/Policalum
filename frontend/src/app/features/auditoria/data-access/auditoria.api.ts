// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class AuditoriaApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1AuditoriaDashboard(params: QueryMap = {}): Observable<Api.AuditoriaDashboardDTO> { return this.api.execute<Api.AuditoriaDashboardDTO>(ENDPOINT_BY_ID["GET_api_v1_auditoria_dashboard"], params, params); }
  GetApiV1AuditoriaLogs(params: QueryMap = {}): Observable<Api.PageLogAuditoriaResponseDTO> { return this.api.execute<Api.PageLogAuditoriaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_auditoria_logs"], params, params); }
  GetApiV1AuditoriaLogsEntidadTablaRegistroId(params: QueryMap = {}): Observable<Array<Api.LogAuditoriaResponseDTO>> { return this.api.execute<Array<Api.LogAuditoriaResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_auditoria_logs_entidad_tabla_registroId"], params, params); }
  GetApiV1AuditoriaLogsExportar(params: QueryMap = {}): Observable<string> { return this.api.execute<string>(ENDPOINT_BY_ID["GET_api_v1_auditoria_logs_exportar"], params, params); }
  GetApiV1AuditoriaLogsUsuarioUserId(params: QueryMap = {}): Observable<Api.PageLogAuditoriaResponseDTO> { return this.api.execute<Api.PageLogAuditoriaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_auditoria_logs_usuario_userId"], params, params); }
  GetApiV1AuditoriaLogsId(params: QueryMap = {}): Observable<Api.LogAuditoriaResponseDTO> { return this.api.execute<Api.LogAuditoriaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_auditoria_logs_id"], params, params); }
}
