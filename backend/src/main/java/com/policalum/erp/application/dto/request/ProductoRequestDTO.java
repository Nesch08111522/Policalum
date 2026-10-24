package com.policalum.erp.application.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.policalum.erp.domain.enums.TipoProducto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductoRequestDTO(
        // Cambiado de Entidades JPA a UUIDs
        UUID categoriaId,
        UUID proveedorId,

        @Size(max = 50, message = "El SKU no puede exceder 50 caracteres")
        String codigoSku,

        @NotBlank(message = "El nombre del producto es requerido")
        @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
        String nombre,

        String descripcion,

        @NotNull(message = "El tipo de producto es requerido")
        TipoProducto tipo,

        @NotNull(message = "El precio de compra es requerido")
        @PositiveOrZero(message = "El precio de compra debe ser igual o mayor a cero")
        BigDecimal precioCompra,

        @NotNull(message = "El precio de venta base es requerido")
        @PositiveOrZero(message = "El precio de venta base debe ser igual o mayor a cero")
        BigDecimal precioVentaBase,

        @Size(max = 20, message = "La unidad no puede exceder 20 caracteres")
        String unidad,

        @NotNull(message = "El estado de fraccionabilidad es requerido")
        Boolean esFraccionable,

        @NotNull(message = "El estado del producto es requerido")
        Boolean activo,

        Boolean incluyeIgv,

        @Size(max = 10, message = "El tipo de afectación no puede exceder 10 caracteres")
        String tipoAfectacion,

        // CAMPO FALTANTE: Lista de fracciones para la API
        @Valid
        List<CatalogoFraccionRequestDTO> fracciones,

        @Size(max = 500, message = "La imagen no puede exceder 500 caracteres")
        String imagen
) {
}