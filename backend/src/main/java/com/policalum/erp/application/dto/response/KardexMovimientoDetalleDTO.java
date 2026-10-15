package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.TipoMovimiento;
import com.policalum.erp.domain.enums.TipoReferencia;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un movimiento del Kardex sin entidades anidadas.
 * stockAnterior = stockResultante ∓ cantidadMovida. createdAt null = movimiento antiguo sin fecha.
 */
public record KardexMovimientoDetalleDTO(
        UUID id,
        LocalDateTime createdAt,
        TipoMovimiento tipoMovimiento,
        TipoReferencia tipoReferencia,
        UUID referenciaId,
        UUID productoId,
        String productoNombre,
        String codigoSku,
        String unidad,
        UUID almacenId,
        String almacenNombre,
        UUID sedeId,
        String sedeNombre,
        BigDecimal cantidadMovida,
        BigDecimal stockAnterior,
        BigDecimal stockResultante,
        UUID userId,
        String userNombre,
        String observacion) { }
