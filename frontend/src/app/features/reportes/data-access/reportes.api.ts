// Servicio generado desde backend/docs/openapi.json. No editar manualmente.
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';
import * as Api from '../../../core/api/openapi.types';

@Injectable({ providedIn: 'root' })
export class ReportesApiService {
  constructor(private readonly api: BackendApiService) {}

  GetApiV1ReportesCajaArqueos(params: QueryMap = {}): Observable<Api.ArqueosReporteDTO> { return this.api.execute<Api.ArqueosReporteDTO>(ENDPOINT_BY_ID["GET_api_v1_reportes_caja_arqueos"], params, params); }
  GetApiV1ReportesCajaArqueosExportar(params: QueryMap = {}): Observable<string> { return this.api.execute<string>(ENDPOINT_BY_ID["GET_api_v1_reportes_caja_arqueos_exportar"], params, params); }
  GetApiV1ReportesClientesFrecuentes(params: QueryMap = {}): Observable<Api.ClientesFrecuentesDTO> { return this.api.execute<Api.ClientesFrecuentesDTO>(ENDPOINT_BY_ID["GET_api_v1_reportes_clientes_frecuentes"], params, params); }
  GetApiV1ReportesClientesFrecuentesExportar(params: QueryMap = {}): Observable<string> { return this.api.execute<string>(ENDPOINT_BY_ID["GET_api_v1_reportes_clientes_frecuentes_exportar"], params, params); }
  GetApiV1ReportesProductosEstrella(params: QueryMap = {}): Observable<Api.ProductosEstrellaDTO> { return this.api.execute<Api.ProductosEstrellaDTO>(ENDPOINT_BY_ID["GET_api_v1_reportes_productos_estrella"], params, params); }
  GetApiV1ReportesProductosRentabilidad(params: QueryMap = {}): Observable<Api.RentabilidadReporteDTO> { return this.api.execute<Api.RentabilidadReporteDTO>(ENDPOINT_BY_ID["GET_api_v1_reportes_productos_rentabilidad"], params, params); }
  GetApiV1ReportesProductosRentabilidadExportar(params: QueryMap = {}): Observable<string> { return this.api.execute<string>(ENDPOINT_BY_ID["GET_api_v1_reportes_productos_rentabilidad_exportar"], params, params); }
  GetApiV1ReportesProveedoresRanking(params: QueryMap = {}): Observable<Api.ProveedoresRankingDTO> { return this.api.execute<Api.ProveedoresRankingDTO>(ENDPOINT_BY_ID["GET_api_v1_reportes_proveedores_ranking"], params, params); }
  GetApiV1ReportesProveedoresRankingExportar(params: QueryMap = {}): Observable<string> { return this.api.execute<string>(ENDPOINT_BY_ID["GET_api_v1_reportes_proveedores_ranking_exportar"], params, params); }
  GetApiV1ReportesVentas(params: QueryMap = {}): Observable<Api.PageVentaReporteDTO> { return this.api.execute<Api.PageVentaReporteDTO>(ENDPOINT_BY_ID["GET_api_v1_reportes_ventas"], params, params); }
  GetApiV1ReportesVentasExportar(params: QueryMap = {}): Observable<string> { return this.api.execute<string>(ENDPOINT_BY_ID["GET_api_v1_reportes_ventas_exportar"], params, params); }
  GetApiV1ReportesVentasResumen(params: QueryMap = {}): Observable<Api.VentasResumenDTO> { return this.api.execute<Api.VentasResumenDTO>(ENDPOINT_BY_ID["GET_api_v1_reportes_ventas_resumen"], params, params); }
}
