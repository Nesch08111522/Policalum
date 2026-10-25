package com.policalum.erp.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SedeResponseDTO(
        UUID id,
        String nombre,
        String pais,
        String ciudad,
        String direccion,
        String celularSede,
        String moneda,
        Boolean activa,
        LocalDateTime createdAt,
        Double latitud,
        Double longitud,
        String imagen) {
}
