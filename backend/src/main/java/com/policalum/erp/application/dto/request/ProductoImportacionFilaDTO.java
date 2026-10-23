package com.policalum.erp.application.dto.request;

import java.math.BigDecimal;

/**
 * Fila de importación/exportación. Solo campos que el catálogo ya persiste,
 * más el stock inicial (movimiento de kardex) y el almacén existente donde entra.
 */
public record ProductoImportacionFilaDTO(
        String codigoSku,
        String nombre,
        String descripcion,
        String categoria,
        String tipo,
        BigDecimal precioCompra,
        BigDecimal precioVentaBase,
        String unidad,
        Boolean esFraccionable,
        Boolean activo,
        Boolean incluyeIgv,
        String tipoAfectacion,
        String imagen,
        BigDecimal stockInicial,
        String almacen
) {
}
