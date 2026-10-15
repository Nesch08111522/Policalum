package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.AuditoriaDashboardDTO;
import com.policalum.erp.application.dto.response.LogAuditoriaResponseDTO;
import com.policalum.erp.domain.enums.AccionAudit;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Consulta del log de auditoría (Módulo 13). Solo ADMIN. Es de solo lectura: los logs los
 * escribe AuditService.registrar y no se editan ni se borran.
 * Fechas inclusivas [desde, hasta]; sin fechas = todo el historial.
 */
public interface AuditoriaService {

    /** Logs filtrados, del más reciente al más antiguo. */
    Page<LogAuditoriaResponseDTO> buscar(String tabla, AccionAudit accion, UUID userId,
                                         LocalDate desde, LocalDate hasta, Pageable pageable);

    /** Detalle de un log. 404 si no existe. */
    LogAuditoriaResponseDTO obtener(UUID id);

    /** Logs de un usuario. 404 si el usuario no existe. */
    Page<LogAuditoriaResponseDTO> porUsuario(UUID userId, AccionAudit accion, LocalDate desde, LocalDate hasta, Pageable pageable);

    /** Historial completo de un registro (p. ej. tabla "ventas" + id de la venta), del más reciente al más antiguo. */
    List<LogAuditoriaResponseDTO> porEntidad(String tabla, UUID registroId);

    /**
     * CSV (UTF-8 con BOM, separado por comas) con los mismos filtros que buscar.
     * 400 si el resultado supera el máximo exportable. La exportación queda auditada.
     */
    ArchivoDescargaDTO exportarCsv(String tabla, AccionAudit accion, UUID userId, LocalDate desde, LocalDate hasta);

    /** KPIs de actividad. Sin fechas: últimos 30 días. Rango máximo: 366 días. */
    AuditoriaDashboardDTO dashboard(LocalDate desde, LocalDate hasta);
}
