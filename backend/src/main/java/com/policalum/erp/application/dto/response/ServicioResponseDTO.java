package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ServicioResponseDTO(
        UUID id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Boolean activo,
        LocalDateTime createdAt
) {
}
