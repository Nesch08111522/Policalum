package com.policalum.erp.application.dto.response;

import java.util.UUID;

public record ServicioColaboradorResponseDTO(
        UUID userId,
        String nombre,
        String apellido,
        String email,
        String participacion
) {
}
