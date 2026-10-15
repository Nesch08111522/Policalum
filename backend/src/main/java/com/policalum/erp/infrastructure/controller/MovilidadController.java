package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.AsignarConductorRequestDTO;
import com.policalum.erp.application.dto.request.MovilidadRequestDTO;
import com.policalum.erp.application.dto.response.MovilidadDashboardDTO;
import com.policalum.erp.application.dto.response.MovilidadListadoDTO;
import com.policalum.erp.application.dto.response.MovilidadResponseDTO;
import com.policalum.erp.application.dto.response.OrdenDespachoResponseDTO;
import com.policalum.erp.application.dto.response.ReporteUsoMovilidadDTO;
import com.policalum.erp.application.service.MovilidadService;
import com.policalum.erp.domain.enums.EstadoDespacho;
import com.policalum.erp.domain.enums.TipoMovilidad;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Vehículos de reparto (Módulo 16). Lectura: ADMIN y MULTIFUNCIONAL. Escritura: solo ADMIN. */
@Tag(name = "Movilidad")
@RestController
@RequestMapping("/api/v1/movilidades")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
public class MovilidadController {

    private final MovilidadService service;

    /** Crea un vehículo. 201 + Location. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovilidadResponseDTO> crear(@Valid @RequestBody MovilidadRequestDTO request) {
        MovilidadResponseDTO creada = service.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    /** Lista con filtros: sede, activo, tipo y texto (placa, marca, modelo o conductor). */
    @GetMapping
    public ResponseEntity<Page<MovilidadListadoDTO>> listar(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) TipoMovilidad tipo,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.listar(sedeId, activo, tipo, texto, pageable));
    }

    /** Uso por vehículo: entregados y fallidos por fecha programada (sin fechas: mes en curso). */
    @GetMapping("/reportes/uso")
    public ResponseEntity<ReporteUsoMovilidadDTO> reporteUso(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.reporteUso(sedeId, desde, hasta));
    }

    /** KPIs de la flota. */
    @GetMapping("/dashboard")
    public ResponseEntity<MovilidadDashboardDTO> dashboard(@RequestParam(required = false) UUID sedeId) {
        return ResponseEntity.ok(service.dashboard(sedeId));
    }

    /** Detalle. */
    @GetMapping("/{id}")
    public ResponseEntity<MovilidadResponseDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    /** Edita datos; activo=false desactiva (mismas validaciones que DELETE). */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovilidadResponseDTO> actualizar(@PathVariable UUID id, @Valid @RequestBody MovilidadRequestDTO request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    /** Baja lógica. 409 si tiene despachos PENDIENTE o EN_CAMINO. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    /** Asigna conductor (MULTIFUNCIONAL activo) o lo quita con conductorId null. */
    @PostMapping("/{id}/asignar-conductor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovilidadResponseDTO> asignarConductor(@PathVariable UUID id,
                                                                 @RequestBody(required = false) AsignarConductorRequestDTO request) {
        return ResponseEntity.ok(service.asignarConductor(id, request));
    }

    /** Despachos del vehículo, más recientes primero. */
    @GetMapping("/{id}/historial")
    public ResponseEntity<Page<OrdenDespachoResponseDTO>> historial(@PathVariable UUID id,
                                                                   @RequestParam(required = false) EstadoDespacho estado,
                                                                   @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.historial(id, estado, pageable));
    }
}
