package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record FacturacionElectronicaPanelDTO(
        String ruc,
        boolean rucEmpresa20,
        String razonSocial,
        String enlaceCodigo,
        String enlaceMensaje,
        long boletas,
        long facturas,
        long notasCredito,
        long aceptados,
        long conObservacion,
        long rechazados,
        List<ComprobanteSunatDTO> historial
) {
    public record ComprobanteSunatDTO(
            String serie,
            String documentoCliente,
            String estadoCdr,
            BigDecimal monto
    ) {}
}
