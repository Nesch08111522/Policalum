package com.policalum.erp.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacionResponseDTO(
        UUID id,
        String tipo,
        String titulo,
        String detalle,
        String ruta,
        boolean leida,
        boolean eliminada,
        LocalDateTime createdAt
) {}
