package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.AprobarDevolucionRequestDTO;
import com.policalum.erp.application.dto.request.DevolucionRequestDTO;
import com.policalum.erp.application.dto.request.RechazarDevolucionRequestDTO;
import com.policalum.erp.application.dto.response.DevolucionDashboardDTO;
import com.policalum.erp.application.dto.response.DevolucionListadoDTO;
import com.policalum.erp.application.dto.response.DevolucionResponseDTO;
import com.policalum.erp.application.service.DevolucionService;
import com.policalum.erp.domain.enums.EstadoDevolucion;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Devoluciones. El personal registra y consulta; aprobar, rechazar y el dashboard son solo ADMIN. */
@Tag(name = "Devoluciones")
@RestController
@RequestMapping("/api/v1/devoluciones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
public class DevolucionController {

    private final DevolucionService devolucionService;

    /** Registra una devolución PENDIENTE. 201 + Location. */
    @PostMapping
    public ResponseEntity<DevolucionResponseDTO> crear(@Valid @RequestBody DevolucionRequestDTO request) {
        DevolucionResponseDTO creada = devolucionService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    /** KPIs del periodo (por defecto, mes en curso). */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucionDashboardDTO> dashboard(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(devolucionService.dashboard(sedeId, desde, hasta));
    }

    /** Devoluciones de una venta. */
    @GetMapping("/venta/{ventaId}")
    public ResponseEntity<List<DevolucionListadoDTO>> porVenta(@PathVariable UUID ventaId) {
        return ResponseEntity.ok(devolucionService.porVenta(ventaId));
    }

    /** Listado paginado; fechas inclusivas sobre la fecha de solicitud. */
    @GetMapping
    public ResponseEntity<Page<DevolucionListadoDTO>> buscar(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) EstadoDevolucion estado,
            @RequestParam(required = false) UUID ventaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fechaSolicitud", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(devolucionService.buscar(sedeId, estado, ventaId, desde, hasta, pageable));
    }

    /** Detalle con líneas y estado del reembolso. */
    @GetMapping("/{id}")
    public ResponseEntity<DevolucionResponseDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(devolucionService.obtener(id));
    }

    /** Aprueba y mueve Kardex (BUENO reingresa; DAÑADO/USADO queda como merma). Cuerpo opcional. */
    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucionResponseDTO> aprobar(@PathVariable UUID id,
                                                         @Valid @RequestBody(required = false) AprobarDevolucionRequestDTO request) {
        return ResponseEntity.ok(devolucionService.aprobar(id, request));
    }

    /** Rechaza con motivo obligatorio. */
    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucionResponseDTO> rechazar(@PathVariable UUID id,
                                                          @Valid @RequestBody RechazarDevolucionRequestDTO request) {
        return ResponseEntity.ok(devolucionService.rechazar(id, request));
    }
}
