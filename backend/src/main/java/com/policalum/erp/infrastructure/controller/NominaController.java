package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.policalum.erp.application.dto.request.GenerarNominaRequestDTO;
import com.policalum.erp.application.dto.request.PagarNominaRequestDTO;
import com.policalum.erp.application.dto.response.BoletaPagoDTO;
import com.policalum.erp.application.dto.response.GeneracionNominaResponseDTO;
import com.policalum.erp.application.dto.response.NominaDashboardDTO;
import com.policalum.erp.application.dto.response.NominaListadoDTO;
import com.policalum.erp.application.dto.response.NominaResponseDTO;
import com.policalum.erp.application.service.NominaService;
import com.policalum.erp.domain.enums.EstadoNomina;
import jakarta.validation.Valid;
import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Nóminas. Todo es ADMIN salvo la boleta, que el empleado puede ver si es suya. */
@Tag(name = "RRHH")
@RestController
@RequestMapping("/api/v1/rrhh/nominas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NominaController {

    private final NominaService nominaService;

    /** Genera las nóminas PENDIENTES de una sede para un periodo. 201 + Location al listado del periodo. */
    @Operation(summary = "Generar nóminas", description = "Genera las nóminas PENDIENTES de una sede para un periodo (yyyy-MM) a partir de legajos activos, asistencia y comisiones (ONP 13% o AFP según el legajo). Los empleados que ya tienen nómina del periodo se omiten. Solo ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado (header Location con la URL del recurso)"),
            @ApiResponse(responseCode = "404", description = "Sede no encontrada"),
            @ApiResponse(responseCode = "409", description = "Hay comisiones CALCULADAS sin aprobar en el periodo")
    })
    @PostMapping("/generar")
    public ResponseEntity<GeneracionNominaResponseDTO> generar(@Valid @RequestBody GenerarNominaRequestDTO request) {
        GeneracionNominaResponseDTO resultado = nominaService.generar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/rrhh/nominas")
                .queryParam("periodo", resultado.periodo())
                .queryParam("sedeId", resultado.sedeId())
                .build().toUri();
        return ResponseEntity.created(location).body(resultado);
    }

    /** Totales del periodo por estado. Sin periodo: mes en curso. */
    @Operation(summary = "Dashboard de nóminas", description = "Totales del periodo por estado (mes en curso por defecto). Solo ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping("/dashboard")
    public ResponseEntity<NominaDashboardDTO> dashboard(@RequestParam(required = false) String periodo,
                                                        @RequestParam(required = false) UUID sedeId) {
        return ResponseEntity.ok(nominaService.dashboard(periodo, sedeId));
    }

    /** Listado paginado con filtros opcionales. */
    @Operation(summary = "Buscar nóminas", description = "Listado paginado con filtros opcionales. Solo ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping
    public ResponseEntity<Page<NominaListadoDTO>> buscar(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) EstadoNomina estado,
            @RequestParam(required = false) UUID userId,
            @PageableDefault(size = 20, sort = "periodo", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(nominaService.buscar(periodo, sedeId, estado, userId, pageable));
    }

    /** Detalle de una nómina. */
    @Operation(summary = "Detalle de nómina", description = "Solo ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Nómina no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<NominaResponseDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(nominaService.obtener(id));
    }

    /** Boleta de pago. ADMIN o el propio empleado. */
    @Operation(summary = "Boleta de pago", description = "Boleta de la nómina. ADMIN o el propio empleado (MULTIFUNCIONAL solo la suya).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Nómina no encontrada")
    })
    @GetMapping("/{id}/boleta")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<BoletaPagoDTO> boleta(@PathVariable UUID id) {
        return ResponseEntity.ok(nominaService.boleta(id));
    }

    /** PENDIENTE → APROBADA. */
    @Operation(summary = "Aprobar nómina", description = "PENDIENTE → APROBADA. Solo ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Nómina no encontrada"),
            @ApiResponse(responseCode = "409", description = "La nómina no está PENDIENTE")
    })
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<NominaResponseDTO> aprobar(@PathVariable UUID id) {
        return ResponseEntity.ok(nominaService.aprobar(id));
    }

    /** APROBADA → PAGADA (solo cambio de estado, no mueve caja). */
    @Operation(summary = "Pagar nómina", description = "APROBADA → PAGADA (registra el pago; no mueve la caja). Solo ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Nómina no encontrada"),
            @ApiResponse(responseCode = "409", description = "La nómina no está APROBADA")
    })
    @PostMapping("/{id}/pagar")
    public ResponseEntity<NominaResponseDTO> pagar(@PathVariable UUID id,
                                                   @Valid @RequestBody PagarNominaRequestDTO request) {
        return ResponseEntity.ok(nominaService.pagar(id, request));
    }

    /** Elimina una nómina PENDIENTE. 204. */
    @Operation(summary = "Eliminar nómina", description = "Elimina una nómina PENDIENTE. Solo ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sin contenido"),
            @ApiResponse(responseCode = "404", description = "Nómina no encontrada"),
            @ApiResponse(responseCode = "409", description = "Solo se eliminan nóminas PENDIENTES")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        nominaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
