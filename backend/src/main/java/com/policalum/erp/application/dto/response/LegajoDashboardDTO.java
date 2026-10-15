package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * KPIs de RRHH. Los "activosSin..." son alertas de legajos incompletos.
 * usuariosActivosSinLegajo incluye a cualquier usuario activo (también admins).
 */
public record LegajoDashboardDTO(
        UUID sedeId,
        long totalLegajos,
        long activos,
        long inactivos,
        Map<String, Long> activosPorTipoContrato,
        Map<String, Long> activosPorSistemaPension,
        BigDecimal planillaMensualBase,
        BigDecimal sueldoPromedio,
        long activosSinContrato,
        long activosSinDni,
        long activosSinSistemaPension,
        long usuariosActivosSinLegajo,
        long totalDocumentos
) { }
