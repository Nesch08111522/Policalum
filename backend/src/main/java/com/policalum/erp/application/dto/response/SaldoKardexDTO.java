package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Saldo de un producto en un almacén. Sin costos: lo pueden ver ADMIN y MULTIFUNCIONAL. */
public record SaldoKardexDTO(
        UUID productoId,
        String productoNombre,
        String codigoSku,
        String unidad,
        UUID almacenId,
        String almacenNombre,
        UUID sedeId,
        String sedeNombre,
        BigDecimal cantidad,
        BigDecimal stockMinimo,
        boolean bajoMinimo,
        LocalDateTime ultimoMovimiento) { }
