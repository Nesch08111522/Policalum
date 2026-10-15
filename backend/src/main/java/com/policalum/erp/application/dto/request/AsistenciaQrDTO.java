package com.policalum.erp.application.dto.request;

import java.util.UUID;

public record AsistenciaQrDTO(
        String payload,
        UUID empleadoId,
        Long expiraEnSegundos
) {}