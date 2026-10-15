package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * @param tasaDevolucionPorcentaje montoAprobado / ventas COMPLETADAS (con IGV) del periodo × 100
 * @param unidadesReingresadas     unidades de stock devueltas en estado BUENO (aprobadas)
 * @param unidadesMerma            unidades DAÑADAS/USADAS (aprobadas), registradas como merma
 */
public record DevolucionDashboardDTO(
        UUID sedeId,
        LocalDate desde,
        LocalDate hasta,
        Map<String, Long> devolucionesPorEstado,
        BigDecimal montoAprobado,
        BigDecimal ventasDelPeriodo,
        BigDecimal tasaDevolucionPorcentaje,
        long reembolsosPendientes,
        BigDecimal montoReembolsosPendientes,
        BigDecimal montoReembolsado,
        BigDecimal unidadesReingresadas,
        BigDecimal unidadesMerma
) { }
