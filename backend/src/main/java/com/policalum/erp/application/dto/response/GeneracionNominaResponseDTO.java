package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @param omitidas     empleados que ya tenían nómina en el periodo
 * @param advertencias datos incompletos del legajo que el admin debe revisar antes de aprobar
 */
public record GeneracionNominaResponseDTO(
        String periodo,
        UUID sedeId,
        int generadas,
        int omitidas,
        BigDecimal totalNetoGenerado,
        List<String> advertencias,
        List<NominaListadoDTO> nominas
) { }
