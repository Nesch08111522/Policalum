package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.model.Categoria;
import java.util.UUID;

public record CategoriaResponseDTO(
        UUID id,
        Categoria parent,
        String nombre,
        String slug,
        Boolean activa) {
}
