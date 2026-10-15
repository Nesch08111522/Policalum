package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.AccionAudit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public record AuditLogRequestDTO(
        UUID userId, // Solo el ID, no la entidad completa

        @NotBlank(message = "El nombre de la tabla es requerido")
        @Size(max = 100, message = "La tabla no puede superar los 100 caracteres")
        String tabla,

        @NotNull(message = "El ID del registro auditado es requerido")
        UUID registroId,

        @NotNull(message = "La acción de auditoría es requerida")
        AccionAudit accion,

        Map<String, Object> datosJson,

        String userAgent
) {
}