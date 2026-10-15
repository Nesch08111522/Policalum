package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;

/**
 * Indicadores del periodo. Las variaciones comparan con el periodo anterior de igual
 * duración; son null si el periodo anterior es cero (no hay base para el porcentaje).
 */
public record KpisFinancierosDTO(
        BigDecimal ventasNetasReales,
        long cantidadVentas,
        BigDecimal ticketPromedio,
        BigDecimal variacionVentasPorcentaje,
        BigDecimal utilidadNeta,
        BigDecimal margenNetoPorcentaje,
        BigDecimal variacionUtilidadPorcentaje,
        BigDecimal flujoOperativo,
        BigDecimal flujoNeto,
        BigDecimal tasaDevolucionPorcentaje,
        BigDecimal valorMermas,
        BigDecimal retirosSocios,
        BigDecimal retirosSobreUtilidadPorcentaje,
        BigDecimal cuentasPorPagarVencidas,
        BigDecimal conversionPreformasPorcentaje,
        BigDecimal montoPreformasPerdido) { }
