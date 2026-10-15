package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record DetalleCompraResponseDTO(
        UUID id,
        UUID productoId,
        String productoSku,
        String productoNombre,
        UUID fraccionId,
        String fraccionNombre,
        BigDecimal factorFraccion,
        UUID almacenId,
        String almacenNombre,
        BigDecimal cantidad,
        BigDecimal precioUnitarioConIgv,
        BigDecimal valorUnitarioNeto,
        BigDecimal igvLinea,
        BigDecimal subtotal,
        BigDecimal cantidadRecibida
) { }