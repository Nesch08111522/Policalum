package com.policalum.erp.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Reporte de uso en [desde, hasta] (fechas inclusivas), de más a menos entregas. */
public record ReporteUsoMovilidadDTO(
        UUID sedeId,
        LocalDate desde,
        LocalDate hasta,
        long totalEntregados,
        long totalFallidos,
        long despachosSinMovilidad,
        List<UsoMovilidadDTO> vehiculos) { }
