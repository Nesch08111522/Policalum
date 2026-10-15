package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.AuditoriaDashboardDTO;
import com.policalum.erp.application.dto.response.LogAuditoriaResponseDTO;
import com.policalum.erp.application.service.AuditoriaService;
import com.policalum.erp.domain.enums.AccionAudit;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta del log de auditoría (Módulo 13). Solo ADMIN, solo lectura. Fechas inclusivas (yyyy-MM-dd). */
@Tag(name = "Auditoría")
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    /** Logs paginados, más recientes primero. Filtros opcionales: tabla, accion, userId, desde, hasta. */
    @GetMapping("/logs")
    public ResponseEntity<Page<LogAuditoriaResponseDTO>> buscar(
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false) AccionAudit accion,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditoriaService.buscar(tabla, accion, userId, desde, hasta, pageable));
    }

    /** Exporta a CSV los logs con los mismos filtros (máximo 50 000 filas). */
    @GetMapping("/logs/exportar")
    public ResponseEntity<Resource> exportar(
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false) AccionAudit accion,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        ArchivoDescargaDTO archivo = auditoriaService.exportarCsv(tabla, accion, userId, desde, hasta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archivo.nombreOriginal()).build().toString())
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .body(archivo.resource());
    }

    /** Logs de un usuario. 404 si el usuario no existe. */
    @GetMapping("/logs/usuario/{userId}")
    public ResponseEntity<Page<LogAuditoriaResponseDTO>> porUsuario(
            @PathVariable UUID userId,
            @RequestParam(required = false) AccionAudit accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditoriaService.porUsuario(userId, accion, desde, hasta, pageable));
    }

    /** Historial completo de un registro, p. ej. /logs/entidad/ventas/{idVenta}. */
    @GetMapping("/logs/entidad/{tabla}/{registroId}")
    public ResponseEntity<List<LogAuditoriaResponseDTO>> porEntidad(@PathVariable String tabla, @PathVariable UUID registroId) {
        return ResponseEntity.ok(auditoriaService.porEntidad(tabla, registroId));
    }

    /** Detalle de un log. */
    @GetMapping("/logs/{id}")
    public ResponseEntity<LogAuditoriaResponseDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(auditoriaService.obtener(id));
    }

    /** KPIs: eventos por acción y por día, usuarios y tablas más activos. Sin fechas: últimos 30 días. */
    @GetMapping("/dashboard")
    public ResponseEntity<AuditoriaDashboardDTO> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(auditoriaService.dashboard(desde, hasta));
    }
}
