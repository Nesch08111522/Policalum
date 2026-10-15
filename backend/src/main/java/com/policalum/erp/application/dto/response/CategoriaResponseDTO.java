package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.model.Categoria;

public record CategoriaResponseDTO(
        Categoria parent,
        String nombre,
        String slug,
        Boolean activa) {
}
