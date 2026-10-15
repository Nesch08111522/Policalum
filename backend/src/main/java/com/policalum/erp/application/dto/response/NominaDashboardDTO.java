package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record NominaDashboardDTO(
        String periodo,
        UUID sedeId,
        long cantidadPendientes,
        long cantidadAprobadas,
        long cantidadPagadas,
        BigDecimal totalSueldos,
        BigDecimal totalComisiones,
        BigDecimal totalDescuentos,
        BigDecimal totalNeto,
        BigDecimal totalPagado,
        BigDecimal totalPorPagar
) { }
