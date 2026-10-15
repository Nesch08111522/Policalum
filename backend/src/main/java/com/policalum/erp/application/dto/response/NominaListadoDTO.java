package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoNomina;
import java.math.BigDecimal;
import java.util.UUID;

public record NominaListadoDTO(
        UUID id,
        UUID userId,
        String empleadoNombre,
        String sedeNombre,
        String periodo,
        BigDecimal sueldoBaseCalculado,
        BigDecimal totalComisiones,
        BigDecimal descuentos,
        BigDecimal totalNeto,
        EstadoNomina estado
) { }
