package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;

/** Un mes de la serie histórica (devengado + caja). parcial = mes en curso. */
public record MesEvolucionDTO(
        String periodo,
        boolean parcial,
        BigDecimal ventasNetasReales,
        BigDecimal devolucionesNetas,
        BigDecimal comprasNetas,
        BigDecimal nominas,
        BigDecimal utilidadNeta,
        BigDecimal margenNetoPorcentaje,
        BigDecimal cobros,
        BigDecimal egresosOperativos,
        BigDecimal flujoOperativo,
        BigDecimal retirosSocios,
        BigDecimal flujoNeto) { }
