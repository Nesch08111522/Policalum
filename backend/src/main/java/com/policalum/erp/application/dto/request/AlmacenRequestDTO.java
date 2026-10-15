package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.model.Sede;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlmacenRequestDTO(
                @NotNull(message = "La sede es requerida") Sede sede,

                @NotBlank(message = "El nombre del almacén es requerido") @Size(max = 100, message = "El nombre no puede exceder 100 caracteres") String nombre,

                @NotNull(message = "El estado del almacén es requerido") Boolean activo,
                @Size(max = 5000, message = "La descripción no puede exceder 5000 caracteres") String descripcion) {
}
