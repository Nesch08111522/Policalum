package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record MermaPorProductoDTO(
        UUID productoId,
        String productoNombre,
        String codigoSku,
        String unidad,
        long movimientos,
        BigDecimal cantidad,
        BigDecimal valor) { }
