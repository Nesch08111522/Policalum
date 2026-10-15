package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/** Stock de un producto sumado en todos los almacenes del filtro. */
public record StockConsolidadoDTO(
        UUID productoId,
        String productoNombre,
        String codigoSku,
        String unidad,
        BigDecimal stockTotal,
        BigDecimal stockMinimoTotal,
        long almacenes,
        long almacenesBajoMinimo) { }
