package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.model.Categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequestDTO(
        Categoria parent,

        @NotBlank(message = "El nombre de la categoría es requerido") @Size(max = 100, message = "El nombre no puede exceder 100 caracteres") String nombre,

        @NotBlank(message = "El slug es requerido") @Size(max = 100, message = "El slug no puede exceder 100 caracteres") String slug,

        @NotNull(message = "El estado de la categoría es requerido") Boolean activa) {
}
