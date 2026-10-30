package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoMensajeWeb;
import java.time.LocalDateTime;
import java.util.UUID;

public record MensajeContactoResponseDTO(
        UUID id,
        String nombre,
        String correo,
        String telefono,
        String empresa,
        String asunto,
        String mensaje,
        EstadoMensajeWeb estado,
        String notaGestion,
        LocalDateTime createdAt,
        LocalDateTime gestionadoAt
) {}
