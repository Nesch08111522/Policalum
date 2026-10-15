package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Separación dueño/empresa en el año en curso.
 * utilidadRetenida = lo que la empresa ganó y se quedó (utilidad - retiros de socios).
 * Negativa = los socios retiraron más de lo que la empresa ganó (se está consumiendo capital).
 */
public record SeparacionPatrimonialDTO(
        int anio,
        BigDecimal utilidadNetaAnio,
        BigDecimal retirosSociosAnio,
        BigDecimal utilidadRetenidaAnio,
        BigDecimal retirosSociosMesActual,
        List<RetiroSocioResumenDTO> retirosPorSocio) { }
