package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Transferencia con sus dos movimientos de Kardex (salida en origen, entrada en destino). */
public record TransferenciaStockResponseDTO(
        UUID id,
        String codigo,
        UUID productoId,
        String productoNombre,
        String codigoSku,
        String unidad,
        AlmacenResumenDTO origen,
        AlmacenResumenDTO destino,
        boolean entreSedes,
        BigDecimal cantidad,
        String motivo,
        String referenciaDocumento,
        KardexMovimientoDetalleDTO salida,
        KardexMovimientoDetalleDTO entrada,
        UUID userId,
        String userNombre,
        LocalDateTime createdAt) { }
