package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.CalcularComisionesRequestDTO;
import com.policalum.erp.application.dto.response.CalculoComisionesResponseDTO;
import com.policalum.erp.application.dto.response.ComisionResponseDTO;
import com.policalum.erp.application.service.ComisionService;
import com.policalum.erp.domain.enums.EstadoComision;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Comisiones. Todo es ADMIN salvo /empleado/{userId}, que el empleado puede ver si son suyas. */
@Tag(name = "RRHH")
@RestController
@RequestMapping("/api/v1/rrhh/comisiones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ComisionController {

    private final ComisionService comisionService;

    /** Calcula o recalcula las comisiones del periodo. 201 + Location a /periodo. */
    @PostMapping("/calcular")
    public ResponseEntity<CalculoComisionesResponseDTO> calcular(@Valid @RequestBody CalcularComisionesRequestDTO request) {
        CalculoComisionesResponseDTO resultado = comisionService.calcular(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/rrhh/comisiones/periodo")
                .queryParam("periodo", resultado.periodo())
                .build().toUri();
        return ResponseEntity.created(location).body(resultado);
    }

    /** Listado paginado con filtros opcionales. */
    @GetMapping
    public ResponseEntity<Page<ComisionResponseDTO>> buscar(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) EstadoComision estado,
            @PageableDefault(size = 20, sort = "periodo", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(comisionService.buscar(periodo, sedeId, estado, pageable));
    }

    /** Comisiones de un empleado (ADMIN o el propio empleado). */
    @GetMapping("/empleado/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<Page<ComisionResponseDTO>> porEmpleado(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "periodo", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(comisionService.porEmpleado(userId, pageable));
    }

    /** Todas las comisiones de un periodo YYYY-MM. */
    @GetMapping("/periodo")
    public ResponseEntity<List<ComisionResponseDTO>> porPeriodo(@RequestParam String periodo,
                                                                @RequestParam(required = false) UUID sedeId) {
        return ResponseEntity.ok(comisionService.porPeriodo(periodo, sedeId));
    }

    /** CALCULADA → APROBADA. */
    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<ComisionResponseDTO> aprobar(@PathVariable UUID id) {
        return ResponseEntity.ok(comisionService.aprobar(id));
    }
}
