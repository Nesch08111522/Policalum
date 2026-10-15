package com.policalum.erp.application.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServicioRequestDTO(
        @NotBlank(message = "El nombre del servicio es requerido")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String nombre,

        @Size(max = 5000, message = "La descripción no puede exceder 5000 caracteres")
        String descripcion,

        @NotNull(message = "El precio del servicio es requerido")
        BigDecimal precio,

        @NotNull(message = "El estado del servicio es requerido")
        Boolean activo
) {
}
