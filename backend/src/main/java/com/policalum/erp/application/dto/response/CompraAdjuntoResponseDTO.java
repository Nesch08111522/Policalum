package com.policalum.erp.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompraAdjuntoResponseDTO(
        UUID id,
        String tipo,
        String nombreOriginal,
        String urlArchivo,
        String contentType,
        Long tamanoBytes,
        UUID subidoPorId,
        String subidoPorNombre,
        LocalDateTime createdAt
) { }