package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;

/** Desglose congelado en la cotización. La configuración posterior no lo reescribe. */
public record CargosCotizacionDTO(
        String distrito,
        BigDecimal cantidadPiezas,
        BigDecimal subtotalMateriales,
        BigDecimal utilidadPorcentaje,
        BigDecimal utilidadMonto,
        BigDecimal movilidadMonto,
        BigDecimal otrosMonto,
        String otrosDetalle,
        boolean modificada,
        String notaCambio
) {}
