package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una salida por merma. valor = cantidad × costo promedio ACTUAL del producto (el Kardex no
 * guarda el costo del momento). referenciaId = devolución u otro documento de origen.
 */
public record MermaKardexDTO(
        UUID movimientoId,
        LocalDateTime fecha,
        UUID productoId,
        String productoNombre,
        String codigoSku,
        String unidad,
        UUID almacenId,
        String almacenNombre,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal valor,
        UUID referenciaId,
        String observacion,
        String userNombre) { }
