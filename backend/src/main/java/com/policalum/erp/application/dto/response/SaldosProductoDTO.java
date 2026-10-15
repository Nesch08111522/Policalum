package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Saldos de un producto en todos los almacenes donde tiene stock o movimientos. */
public record SaldosProductoDTO(
        UUID productoId,
        String productoNombre,
        String codigoSku,
        String unidad,
        BigDecimal stockTotal,
        List<SaldoKardexDTO> almacenes) { }
