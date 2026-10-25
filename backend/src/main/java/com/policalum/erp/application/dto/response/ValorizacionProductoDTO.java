package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/** valorTotal = cantidad × costoUnitario (costo promedio ponderado del producto). */
public record ValorizacionProductoDTO(
        UUID productoId,
        String productoNombre,
        String codigoSku,
        String unidad,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal valorTotal,
        BigDecimal porcentajeDelTotal,
        long almacenes,
        BigDecimal precioVenta,
        BigDecimal gananciaEstimada) { }
