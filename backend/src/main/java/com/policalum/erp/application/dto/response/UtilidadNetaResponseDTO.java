package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Utilidad del periodo (vista devengada). Montos en PEN y SIN IGV (el IGV no es ingreso ni gasto).
 *
 * devolucionesNetas  = devoluciones APROBADAS del periodo (monto reembolsado si ya se procesó) / 1.18
 * ventasNetasReales  = ventasNetas - devolucionesNetas
 * utilidadBruta      = ventasNetasReales - comprasNetas
 * gastosOperativos   = nóminas PAGADAS del periodo
 * utilidadOperativa  = utilidadBruta - gastosOperativos
 * utilidadNetaReal   = utilidadOperativa
 * margenNeto         = utilidadNetaReal / ventasNetasReales
 * retirosSocios      = informativo, NO resta a la utilidad
 * saldoTrasRetiros   = utilidadNetaReal - retirosSocios (informativo)
 * valorMermas        = informativo, NO resta: el costo de la mercadería ya se registró al comprarla
 */
public record UtilidadNetaResponseDTO(
        UUID sedeId,
        LocalDate desde,
        LocalDate hasta,

        BigDecimal ventasNetas,
        BigDecimal igvVentas,
        BigDecimal ventasTotales,
        long cantidadVentas,

        BigDecimal devolucionesNetas,
        BigDecimal devolucionesTotales,
        long cantidadDevoluciones,
        BigDecimal ventasNetasReales,

        BigDecimal comprasNetas,
        BigDecimal comprasTotales,
        long cantidadCompras,
        long comprasOtraMonedaExcluidas,

        BigDecimal utilidadBruta,
        BigDecimal gastosOperativos,
        long cantidadNominasPagadas,
        BigDecimal utilidadOperativa,
        BigDecimal utilidadNetaReal,
        BigDecimal margenNetoPorcentaje,

        BigDecimal retirosSocios,
        BigDecimal saldoTrasRetiros,

        BigDecimal valorMermas
) { }
