package com.policalum.erp.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProyectoPortafolioResponseDTO(
        UUID id,
        String titulo,
        String descripcion,
        String imagenUrl,
        Integer orden,
        Boolean publicado,
        LocalDateTime createdAt
) {}
