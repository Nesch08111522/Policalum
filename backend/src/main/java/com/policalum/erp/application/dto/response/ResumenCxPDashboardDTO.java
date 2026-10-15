package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;

public record ResumenCxPDashboardDTO(
        long totalCuentas,
        long pendientes,
        long pagadasParcial,
        long pagadas,
        long vencidas,
        BigDecimal montoTotalPorPagar,
        BigDecimal montoTotalPagado,
        BigDecimal montoTotalVencido
) { }