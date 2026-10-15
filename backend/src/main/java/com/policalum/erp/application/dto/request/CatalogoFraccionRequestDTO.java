package com.policalum.erp.application.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CatalogoFraccionRequestDTO(
        @NotNull(message = "El producto es requerido")
        UUID productoId,

        @NotBlank(message = "El nombre de la fracción es requerido")
        @Size(max = 50, message = "El nombre de la fracción no puede exceder 50 caracteres")
        String nombreFraccion,

        @NotNull(message = "El factor de descuento para Kardex es requerido")
        @DecimalMin(value = "0.0001", message = "El factor de descuento debe ser mayor a 0")
        BigDecimal factorDescuentoKardex,

        // Opcional: si viene null, el Service calcula automáticamente (Base * Factor)
        BigDecimal precioVenta,

        Boolean activo
) {
}
