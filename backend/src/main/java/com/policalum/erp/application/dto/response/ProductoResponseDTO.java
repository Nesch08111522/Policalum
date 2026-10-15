package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.policalum.erp.domain.enums.TipoProducto;

public record ProductoResponseDTO(
        UUID id,
        CategoriaSummaryDTO categoria,
        ProveedorSummaryDTO proveedor,
        String codigoSku,
        String nombre,
        String descripcion,
        TipoProducto tipo,
        BigDecimal precioCompra,
        BigDecimal precioVentaBase,
        String unidad,
        Boolean esFraccionable,
        Boolean activo,
        LocalDateTime createdAt,
        Boolean incluyeIgv,
        String tipoAfectacion,
        List<CatalogoFraccionResponseDTO> fracciones
) {
}