package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.TipoMovimiento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** costoUnitario y valor solo se muestran a ADMIN (null para MULTIFUNCIONAL). */
public record AjusteStockResponseDTO(
        UUID id,
        String codigo,
        UUID productoId,
        String productoNombre,
        String codigoSku,
        String unidad,
        UUID almacenId,
        String almacenNombre,
        UUID sedeId,
        String sedeNombre,
        TipoMovimiento tipoMovimiento,
        BigDecimal cantidad,
        BigDecimal stockAnterior,
        BigDecimal stockResultante,
        BigDecimal costoUnitario,
        BigDecimal valor,
        String motivo,
        String justificacion,
        UUID kardexMovimientoId,
        UUID userId,
        String userNombre,
        LocalDateTime createdAt) { }
