package com.policalum.erp.application.dto.response;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.model.AuditLog;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponseDTO(
        UUID id,
        UUID userId,
        String tabla,
        UUID registroId,
        AccionAudit accion,
        Map<String, Object> datosJson,
        String userAgent,
        LocalDateTime createdAt 
) {
}