package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.AccionAudit;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/** Un evento del log de auditoría. userId null = acción del sistema; createdAt null = log antiguo sin fecha. */
public record LogAuditoriaResponseDTO(
        UUID id,
        String tabla,
        UUID registroId,
        AccionAudit accion,
        UUID userId,
        String userNombre,
        String userEmail,
        Map<String, Object> datosJson,
        String userAgent,
        LocalDateTime createdAt) { }
