package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.AsignarTurnoRequestDTO;
import com.policalum.erp.application.dto.request.TurnoTrabajoRequestDTO;
import com.policalum.erp.application.dto.response.AsignacionTurnoResponseDTO;
import com.policalum.erp.application.dto.response.TurnoDashboardDTO;
import com.policalum.erp.application.dto.response.TurnoTrabajoResponseDTO;
import com.policalum.erp.application.service.TurnoTrabajoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

/** Turnos de trabajo. Escritura y listados: ADMIN. El empleado puede ver sus propios turnos. */
@Tag(name = "RRHH")
@RestController
@RequestMapping("/api/v1/rrhh/turnos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TurnoTrabajoController {

    private final TurnoTrabajoService turnoService;

    /** Crea un turno para un empleado. 201 + Location. */
    @PostMapping
    public ResponseEntity<TurnoTrabajoResponseDTO> crear(@Valid @RequestBody TurnoTrabajoRequestDTO request) {
        TurnoTrabajoResponseDTO creado = turnoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    /** Asigna el mismo horario a varios empleados (todo o nada). 201. */
    @PostMapping("/asignar")
    public ResponseEntity<AsignacionTurnoResponseDTO> asignar(@Valid @RequestBody AsignarTurnoRequestDTO request) {
        AsignacionTurnoResponseDTO resultado = turnoService.asignar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/rrhh/turnos").queryParam("activo", true).build().toUri();
        return ResponseEntity.created(location).body(resultado);
    }

    /** KPIs: cobertura por día, empleados sin turno, exceso de 48 h semanales. */
    @GetMapping("/dashboard")
    public ResponseEntity<TurnoDashboardDTO> dashboard(@RequestParam(required = false) UUID sedeId) {
        return ResponseEntity.ok(turnoService.dashboard(sedeId));
    }

    /** Listado paginado con filtros opcionales. */
    @GetMapping
    public ResponseEntity<Page<TurnoTrabajoResponseDTO>> buscar(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String nombreTurno,
            @PageableDefault(size = 20, sort = "horaInicio", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(turnoService.buscar(sedeId, userId, activo, nombreTurno, pageable));
    }

    /** Turnos activos de un empleado. ADMIN o el propio empleado. */
    @GetMapping("/empleado/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<List<TurnoTrabajoResponseDTO>> porEmpleado(@PathVariable UUID userId) {
        return ResponseEntity.ok(turnoService.porEmpleado(userId));
    }

    /** Detalle. ADMIN o el dueño del turno. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<TurnoTrabajoResponseDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(turnoService.obtener(id));
    }

    /** Actualiza un turno. */
    @PutMapping("/{id}")
    public ResponseEntity<TurnoTrabajoResponseDTO> actualizar(@PathVariable UUID id,
                                                              @Valid @RequestBody TurnoTrabajoRequestDTO request) {
        return ResponseEntity.ok(turnoService.actualizar(id, request));
    }

    /** Desactiva el turno (baja lógica). 204. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        turnoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
