package com.policalum.erp.application.dto.request;

import java.math.BigDecimal;

import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.Producto;

import jakarta.validation.constraints.NotNull;

public record StockRequestDTO(
        @NotNull(message = "El producto es requerido")
        Producto producto,

        @NotNull(message = "El almacén es requerido")
        Almacen almacen,

        @NotNull(message = "La cantidad es requerida")
        BigDecimal cantidad,
                
        BigDecimal stockMinimo,
        java.time.LocalDateTime updatedAt
) {
}
