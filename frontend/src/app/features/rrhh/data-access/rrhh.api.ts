// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class RrhhApiService {
  constructor(private readonly api: BackendApiService) {}

  PostApiV1RrhhAsistenciasConfigurarTotpEmpleadoId(params: QueryMap = {}): Observable<Api.TotpConfigDTO> { return this.api.execute<Api.TotpConfigDTO>(ENDPOINT_BY_ID["POST_api_v1_rrhh_asistencias_configurar_totp_empleadoId"], params, params); }
  GetApiV1RrhhAsistenciasDashboard(params: QueryMap = {}): Observable<Api.DashboardAsistenciaDTO> { return this.api.execute<Api.DashboardAsistenciaDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_asistencias_dashboard"], params, params); }
  GetApiV1RrhhAsistenciasEmpleados(params: QueryMap = {}): Observable<Array<Api.UserInfoDTO>> { return this.api.execute<Array<Api.UserInfoDTO>>(ENDPOINT_BY_ID["GET_api_v1_rrhh_asistencias_empleados"], params, params); }
  GetApiV1RrhhAsistenciasEstadoDia(params: QueryMap = {}): Observable<Api.AsistenciaEstadoDTO> { return this.api.execute<Api.AsistenciaEstadoDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_asistencias_estado_dia"], params, params); }
  GetApiV1RrhhAsistenciasListar(params: QueryMap = {}): Observable<Api.PageRrhhAsistenciaResponseDTO> { return this.api.execute<Api.PageRrhhAsistenciaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_asistencias_listar"], params, params); }
  PostApiV1RrhhAsistenciasManual(params: QueryMap = {}, body: Api.RrhhAsistenciaRequestDTO): Observable<Api.RrhhAsistenciaResponseDTO> { return this.api.execute<Api.RrhhAsistenciaResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_rrhh_asistencias_manual"], params, params, body); }
  PostApiV1RrhhAsistenciasMarcarQr(params: QueryMap = {}, body: Api.AsistenciaQrDTO): Observable<Api.RrhhAsistenciaResponseDTO> { return this.api.execute<Api.RrhhAsistenciaResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_rrhh_asistencias_marcar_qr"], params, params, body); }
  PostApiV1RrhhAsistenciasMarcarQrReal(params: QueryMap = {}, body: Api.AsistenciaQrDTO): Observable<Api.RrhhAsistenciaResponseDTO> { return this.api.execute<Api.RrhhAsistenciaResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_rrhh_asistencias_marcar_qr_real"], params, params, body); }
  GetApiV1RrhhAsistenciasMisAsistencias(params: QueryMap = {}): Observable<Api.PageRrhhAsistenciaResponseDTO> { return this.api.execute<Api.PageRrhhAsistenciaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_asistencias_mis_asistencias"], params, params); }
  GetApiV1RrhhAsistenciasQrConfigEmpleadoId(params: QueryMap = {}): Observable<Api.QrPublicoDTO> { return this.api.execute<Api.QrPublicoDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_asistencias_qr_config_empleadoId"], params, params); }
  GetApiV1RrhhAsistenciasQrPublico(params: QueryMap = {}): Observable<Api.QrPublicoDTO> { return this.api.execute<Api.QrPublicoDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_asistencias_qr_publico"], params, params); }
  GetApiV1RrhhAsistenciasUltimaMarcacion(params: QueryMap = {}): Observable<Api.RrhhAsistenciaResponseDTO> { return this.api.execute<Api.RrhhAsistenciaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_asistencias_ultima_marcacion"], params, params); }
  DeleteApiV1RrhhAsistenciasId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_rrhh_asistencias_id"], params, params); }
  GetApiV1RrhhAsistenciasId(params: QueryMap = {}): Observable<Api.RrhhAsistenciaResponseDTO> { return this.api.execute<Api.RrhhAsistenciaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_asistencias_id"], params, params); }
  PutApiV1RrhhAsistenciasId(params: QueryMap = {}, body: Api.RrhhAsistenciaRequestDTO): Observable<Api.RrhhAsistenciaResponseDTO> { return this.api.execute<Api.RrhhAsistenciaResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_rrhh_asistencias_id"], params, params, body); }
  GetApiV1RrhhComisiones(params: QueryMap = {}): Observable<Api.PageComisionResponseDTO> { return this.api.execute<Api.PageComisionResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_comisiones"], params, params); }
  PostApiV1RrhhComisionesCalcular(params: QueryMap = {}, body: Api.CalcularComisionesRequestDTO): Observable<Api.CalculoComisionesResponseDTO> { return this.api.execute<Api.CalculoComisionesResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_rrhh_comisiones_calcular"], params, params, body); }
  GetApiV1RrhhComisionesEmpleadoUserId(params: QueryMap = {}): Observable<Api.PageComisionResponseDTO> { return this.api.execute<Api.PageComisionResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_comisiones_empleado_userId"], params, params); }
  GetApiV1RrhhComisionesPeriodo(params: QueryMap = {}): Observable<Array<Api.ComisionResponseDTO>> { return this.api.execute<Array<Api.ComisionResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_rrhh_comisiones_periodo"], params, params); }
  PatchApiV1RrhhComisionesIdAprobar(params: QueryMap = {}): Observable<Api.ComisionResponseDTO> { return this.api.execute<Api.ComisionResponseDTO>(ENDPOINT_BY_ID["PATCH_api_v1_rrhh_comisiones_id_aprobar"], params, params); }
  GetApiV1RrhhLegajos(params: QueryMap = {}): Observable<Api.PageLegajoListadoDTO> { return this.api.execute<Api.PageLegajoListadoDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_legajos"], params, params); }
  PostApiV1RrhhLegajos(params: QueryMap = {}, body: Api.LegajoRequestDTO): Observable<Api.LegajoResponseDTO> { return this.api.execute<Api.LegajoResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_rrhh_legajos"], params, params, body); }
  GetApiV1RrhhLegajosDashboard(params: QueryMap = {}): Observable<Api.LegajoDashboardDTO> { return this.api.execute<Api.LegajoDashboardDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_legajos_dashboard"], params, params); }
  GetApiV1RrhhLegajosMe(params: QueryMap = {}): Observable<Api.LegajoResponseDTO> { return this.api.execute<Api.LegajoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_legajos_me"], params, params); }
  GetApiV1RrhhLegajosEmpleadoId(params: QueryMap = {}): Observable<Api.LegajoResponseDTO> { return this.api.execute<Api.LegajoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_legajos_empleadoId"], params, params); }
  PutApiV1RrhhLegajosEmpleadoId(params: QueryMap = {}, body: Api.LegajoRequestDTO): Observable<Api.LegajoResponseDTO> { return this.api.execute<Api.LegajoResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_rrhh_legajos_empleadoId"], params, params, body); }
  GetApiV1RrhhLegajosIdDocumentos(params: QueryMap = {}): Observable<Array<Api.LegajoDocumentoResponseDTO>> { return this.api.execute<Array<Api.LegajoDocumentoResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_rrhh_legajos_id_documentos"], params, params); }
  PostApiV1RrhhLegajosIdDocumentos(params: QueryMap = {}): Observable<Api.LegajoDocumentoResponseDTO> { return this.api.execute<Api.LegajoDocumentoResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_rrhh_legajos_id_documentos"], params, params); }
  DeleteApiV1RrhhLegajosIdDocumentosDocId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_rrhh_legajos_id_documentos_docId"], params, params); }
  GetApiV1RrhhLegajosIdDocumentosDocId(params: QueryMap = {}): Observable<string> { return this.api.execute<string>(ENDPOINT_BY_ID["GET_api_v1_rrhh_legajos_id_documentos_docId"], params, params); }
  GetApiV1RrhhNominas(params: QueryMap = {}): Observable<Api.PageNominaListadoDTO> { return this.api.execute<Api.PageNominaListadoDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_nominas"], params, params); }
  GetApiV1RrhhNominasDashboard(params: QueryMap = {}): Observable<Api.NominaDashboardDTO> { return this.api.execute<Api.NominaDashboardDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_nominas_dashboard"], params, params); }
  PostApiV1RrhhNominasGenerar(params: QueryMap = {}, body: Api.GenerarNominaRequestDTO): Observable<Api.GeneracionNominaResponseDTO> { return this.api.execute<Api.GeneracionNominaResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_rrhh_nominas_generar"], params, params, body); }
  DeleteApiV1RrhhNominasId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_rrhh_nominas_id"], params, params); }
  GetApiV1RrhhNominasId(params: QueryMap = {}): Observable<Api.NominaResponseDTO> { return this.api.execute<Api.NominaResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_nominas_id"], params, params); }
  PostApiV1RrhhNominasIdAprobar(params: QueryMap = {}): Observable<Api.NominaResponseDTO> { return this.api.execute<Api.NominaResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_rrhh_nominas_id_aprobar"], params, params); }
  GetApiV1RrhhNominasIdBoleta(params: QueryMap = {}): Observable<Api.BoletaPagoDTO> { return this.api.execute<Api.BoletaPagoDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_nominas_id_boleta"], params, params); }
  PostApiV1RrhhNominasIdPagar(params: QueryMap = {}, body: Api.PagarNominaRequestDTO): Observable<Api.NominaResponseDTO> { return this.api.execute<Api.NominaResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_rrhh_nominas_id_pagar"], params, params, body); }
  GetApiV1RrhhTurnos(params: QueryMap = {}): Observable<Api.PageTurnoTrabajoResponseDTO> { return this.api.execute<Api.PageTurnoTrabajoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_turnos"], params, params); }
  PostApiV1RrhhTurnos(params: QueryMap = {}, body: Api.TurnoTrabajoRequestDTO): Observable<Api.TurnoTrabajoResponseDTO> { return this.api.execute<Api.TurnoTrabajoResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_rrhh_turnos"], params, params, body); }
  PostApiV1RrhhTurnosAsignar(params: QueryMap = {}, body: Api.AsignarTurnoRequestDTO): Observable<Api.AsignacionTurnoResponseDTO> { return this.api.execute<Api.AsignacionTurnoResponseDTO>(ENDPOINT_BY_ID["POST_api_v1_rrhh_turnos_asignar"], params, params, body); }
  GetApiV1RrhhTurnosDashboard(params: QueryMap = {}): Observable<Api.TurnoDashboardDTO> { return this.api.execute<Api.TurnoDashboardDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_turnos_dashboard"], params, params); }
  GetApiV1RrhhTurnosEmpleadoUserId(params: QueryMap = {}): Observable<Array<Api.TurnoTrabajoResponseDTO>> { return this.api.execute<Array<Api.TurnoTrabajoResponseDTO>>(ENDPOINT_BY_ID["GET_api_v1_rrhh_turnos_empleado_userId"], params, params); }
  DeleteApiV1RrhhTurnosId(params: QueryMap = {}): Observable<unknown> { return this.api.execute<unknown>(ENDPOINT_BY_ID["DELETE_api_v1_rrhh_turnos_id"], params, params); }
  GetApiV1RrhhTurnosId(params: QueryMap = {}): Observable<Api.TurnoTrabajoResponseDTO> { return this.api.execute<Api.TurnoTrabajoResponseDTO>(ENDPOINT_BY_ID["GET_api_v1_rrhh_turnos_id"], params, params); }
  PutApiV1RrhhTurnosId(params: QueryMap = {}, body: Api.TurnoTrabajoRequestDTO): Observable<Api.TurnoTrabajoResponseDTO> { return this.api.execute<Api.TurnoTrabajoResponseDTO>(ENDPOINT_BY_ID["PUT_api_v1_rrhh_turnos_id"], params, params, body); }
}
