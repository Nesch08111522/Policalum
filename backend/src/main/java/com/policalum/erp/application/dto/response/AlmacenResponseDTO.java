package com.policalum.erp.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlmacenResponseDTO(
        UUID id,
        SedeResponseDTO sede,
        String nombre,
        Boolean activo,
        String descripcion,
        LocalDateTime createdAt

) {
}
