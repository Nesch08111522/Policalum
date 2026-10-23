import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BackendApiService, QueryMap } from '../../../core/api/backend-api.service';
import { ENDPOINT_BY_ID } from '../../../core/api/api-endpoints.generated';

export interface ComprobanteSunat {
  serie?: string;
  documentoCliente?: string;
  estadoCdr?: string;
  monto?: number;
}

export interface FacturacionElectronicaPanel {
  ruc?: string;
  rucEmpresa20?: boolean;
  razonSocial?: string;
  enlaceCodigo?: string;
  enlaceMensaje?: string;
  boletas?: number;
  facturas?: number;
  notasCredito?: number;
  aceptados?: number;
  conObservacion?: number;
  rechazados?: number;
  historial?: ComprobanteSunat[];
}

@Injectable({ providedIn: 'root' })
export class FacturacionElectronicaApiService {
  constructor(private readonly api: BackendApiService) {}

  panel(params: QueryMap = {}): Observable<FacturacionElectronicaPanel> {
    return this.api.execute<FacturacionElectronicaPanel>(ENDPOINT_BY_ID['GET_api_v1_facturacion_electronica_panel'], params, params);
  }

  emitir(): Observable<unknown> {
    return this.api.execute(ENDPOINT_BY_ID['POST_api_v1_facturacion_electronica_emitir']);
  }

  consultarValidez(serie: string, documentoCliente: string): Observable<unknown> {
    return this.api.execute(ENDPOINT_BY_ID['POST_api_v1_facturacion_electronica_consultar_validez'], {}, {}, { serie, documentoCliente });
  }
}
