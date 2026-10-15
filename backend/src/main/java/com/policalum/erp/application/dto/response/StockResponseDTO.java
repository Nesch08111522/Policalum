package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.Producto;

public record StockResponseDTO(
        Producto producto,
        Almacen almacen,
        BigDecimal cantidad,
        BigDecimal stockMinimo,
        LocalDateTime updatedAt
) {
}
