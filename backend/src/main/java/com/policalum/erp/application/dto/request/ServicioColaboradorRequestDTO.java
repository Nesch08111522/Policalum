package com.policalum.erp.application.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ServicioColaboradorRequestDTO(
        @NotNull(message = "El usuario del equipo es requerido")
        UUID userId,
        String participacion
) {
}
