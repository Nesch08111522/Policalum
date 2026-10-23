package com.policalum.erp.application.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductoImportacionRequestDTO(
        @NotNull(message = "La lista de filas es requerida")
        @Size(min = 1, max = 2000, message = "La importación admite entre 1 y 2000 filas")
        List<ProductoImportacionFilaDTO> filas
) {
}
