package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.model.AuditLog;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.AuditLogRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * IMPORTANTE: Propagation.REQUIRES_NEW asegura que el log de auditoría se persista
     * incluso si la transacción principal hace rollback. Así no perdemos el rastro.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String tabla, UUID registroId, AccionAudit accion,
                          User user, String userAgent, Map<String, Object> datosJson) {
        try {
            AuditLog log = AuditLog.builder()
                    .tabla(tabla)
                    .registroId(registroId)
                    .accion(accion)
                    .user(user)
                    .userAgent(userAgent)
                    .datosJson(datosJson)
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Nunca dejar que un fallo de auditoría rompa la transacción de negocio
            AuditServiceImpl.log.error("Error registrando auditoría para tabla={}, id={}",
                    tabla, registroId, e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String tabla, UUID registroId, AccionAudit accion,
                          Map<String, Object> datosJson) {
        User user = obtenerUsuarioAutenticado();
        String userAgent = obtenerUserAgent();
        registrar(tabla, registroId, accion, user, userAgent, datosJson);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> search(String tabla, UUID registroId, UUID userId,
                                            AccionAudit accion, LocalDateTime desde,
                                            LocalDateTime hasta, Pageable pageable) {
        return auditLogRepository.search(tabla, registroId, userId, accion, desde, hasta, pageable)
                .map(a -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", a.getId());
                    row.put("tabla", a.getTabla());
                    row.put("registroId", a.getRegistroId());
                    row.put("accion", a.getAccion());
                    row.put("userId", a.getUser() != null ? a.getUser().getId() : null);
                    row.put("userNombre", a.getUser() != null
                            ? a.getUser().getNombre() + " " + a.getUser().getApellido() : null);
                    row.put("datosJson", a.getDatosJson());
                    row.put("userAgent", a.getUserAgent());
                    row.put("createdAt", a.getCreatedAt());
                    return row;
                });
    }

    // ============ MÉTODOS PRIVADOS ============

    private User obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmailAndActivoTrue(auth.getName()).orElse(null);
    }

    private String obtenerUserAgent() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ua = request.getHeader("User-Agent");
                return ua != null && ua.length() > 1000 ? ua.substring(0, 1000) : ua;
            }
        } catch (Exception e) {
            // Sin contexto HTTP (por ejemplo, ejecución en background)
        }
        return null;
    }
}
