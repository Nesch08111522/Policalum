package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;

/** Totales del arqueo sobre TODOS los turnos filtrados (no solo la página). */
public record ResumenArqueosDTO(
        long turnos,
        long abiertos,
        long cerrados,
        long cuadrados,
        long conFaltante,
        long conSobrante,
        BigDecimal totalFaltante,
        BigDecimal totalSobrante,
        BigDecimal diferenciaNeta,
        BigDecimal efectivoVentasAnuladas) { }
