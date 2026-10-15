package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @param nuevas       comisiones creadas
 * @param actualizadas comisiones CALCULADAS recalculadas
 * @param omitidas     comisiones ya APROBADAS o LIQUIDADAS (no se tocan)
 */
public record CalculoComisionesResponseDTO(
        String periodo,
        UUID sedeId,
        int nuevas,
        int actualizadas,
        int omitidas,
        BigDecimal totalComisiones,
        List<String> advertencias,
        List<ComisionResponseDTO> comisiones
) { }
