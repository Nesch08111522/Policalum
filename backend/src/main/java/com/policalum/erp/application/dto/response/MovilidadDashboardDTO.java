package com.policalum.erp.application.dto.response;

import java.util.Map;
import java.util.UUID;

/** KPIs de la flota (sedeId null = toda la empresa). */
public record MovilidadDashboardDTO(
        UUID sedeId,
        long total,
        long activos,
        long inactivos,
        long activosSinConductor,
        long enRuta,
        Map<String, Long> activosPorTipo,
        long entregasHoy) { }
