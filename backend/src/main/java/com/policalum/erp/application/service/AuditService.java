package com.policalum.erp.application.service;

import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public interface AuditService {

    /**
     * Registra una acción de auditoría.
     *
     * @param tabla      Nombre de la tabla afectada (ej. "compras").
     * @param registroId ID del registro afectado.
     * @param accion     Acción ejecutada (INSERT, UPDATE, DELETE, etc.).
     * @param user       Usuario que ejecuta la acción (null si es sistema).
     * @param userAgent  User-Agent del cliente (para forense).
     * @param datosJson  Snapshot de los datos afectados (antes/después).
     */
    void registrar(String tabla, UUID registroId, AccionAudit accion,
                   User user, String userAgent, Map<String, Object> datosJson);

    /**
     * Registra usando el usuario autenticado del SecurityContext.
     */
    void registrar(String tabla, UUID registroId, AccionAudit accion,
                   Map<String, Object> datosJson);

    Page<Map<String, Object>> search(String tabla, UUID registroId, UUID userId,
                                     AccionAudit accion, LocalDateTime desde,
                                     LocalDateTime hasta, Pageable pageable);
}