package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.policalum.erp.application.dto.request.ConvertirCotizacionRequestDTO;
import com.policalum.erp.application.dto.request.CotizacionRequestDTO;
import com.policalum.erp.application.dto.request.RechazarCotizacionRequestDTO;
import com.policalum.erp.application.dto.response.CotizacionListadoDTO;
import com.policalum.erp.application.dto.response.CotizacionResponseDTO;
import com.policalum.erp.application.dto.response.VentaProcesadaResponseDTO;
import com.policalum.erp.application.service.CotizacionService;
import com.policalum.erp.application.service.serviceImpl.CotizacionDocumentoService;
import com.policalum.erp.domain.enums.EstadoCotizacion;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Cotizaciones")
@RestController
@RequestMapping("/api/v1/cotizaciones")
@RequiredArgsConstructor
public class CotizacionController {
    private final CotizacionService cotizacionService;
    private final CotizacionDocumentoService cotizacionDocumento;

    /** Creates an issued quotation with server-calculated amounts. */
    @Operation(summary = "Crear cotización", description = "Emite una cotización (EMITIDA) con montos calculados en el servidor. Sin fecha de vigencia se usa la de Configuración > ventas (15 días por defecto).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado (header Location con la URL del recurso)"),
            @ApiResponse(responseCode = "404", description = "Sede, cliente, producto, fracción o servicio inexistente")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MULTIFUNCIONAL')")
    public ResponseEntity<CotizacionResponseDTO> create(@Valid @RequestBody CotizacionRequestDTO request) {
        CotizacionResponseDTO response = cotizacionService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }
    /** Gets a quotation and its lines. */
    @Operation(summary = "Detalle de cotización", description = "Cotización con sus líneas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada")
    })
    @GetMapping("/{id}") public ResponseEntity<CotizacionResponseDTO> findById(@PathVariable UUID id) { return ResponseEntity.ok(cotizacionService.findById(id)); }
    /** Searches quotations with pagination. */
    @Operation(summary = "Buscar cotizaciones", description = "Listado paginado por cliente, sede, estado y rango de fecha-hora.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping public ResponseEntity<Page<CotizacionListadoDTO>> search(@RequestParam(required=false) UUID clienteId, @RequestParam(required=false) UUID sedeId, @RequestParam(required=false) EstadoCotizacion estado, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta, @PageableDefault(size=20, sort="createdAt") Pageable pageable) { return ResponseEntity.ok(cotizacionService.search(clienteId,sedeId,estado,desde,hasta,pageable)); }
    /** Updates an issued quotation. */
    @Operation(summary = "Editar cotización", description = "Edita una cotización EMITIDA y recalcula montos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada"),
            @ApiResponse(responseCode = "409", description = "Solo se editan cotizaciones EMITIDAS")
    })
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN', 'MULTIFUNCIONAL')") public ResponseEntity<CotizacionResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody CotizacionRequestDTO request) { return ResponseEntity.ok(cotizacionService.update(id, request)); }
    /** Deletes an issued quotation draft. */
    @Operation(summary = "Eliminar cotización", description = "Elimina una cotización EMITIDA.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sin contenido"),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada"),
            @ApiResponse(responseCode = "409", description = "Solo se eliminan cotizaciones EMITIDAS")
    })
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN', 'MULTIFUNCIONAL')") public ResponseEntity<Void> delete(@PathVariable UUID id) { cotizacionService.delete(id); return ResponseEntity.noContent().build(); }
    /** Accepts an issued quotation. */
    @Operation(summary = "Aceptar cotización", description = "EMITIDA → ACEPTADA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada"),
            @ApiResponse(responseCode = "409", description = "Estado inválido o cotización vencida")
    })
    @PostMapping("/{id}/aceptar") @PreAuthorize("hasAnyRole('ADMIN', 'MULTIFUNCIONAL')") public ResponseEntity<CotizacionResponseDTO> aceptar(@PathVariable UUID id) { return ResponseEntity.ok(cotizacionService.aceptar(id)); }
    /** Rejects an issued quotation. */
    @Operation(summary = "Rechazar cotización", description = "EMITIDA → RECHAZADA con motivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada"),
            @ApiResponse(responseCode = "409", description = "Estado inválido")
    })
    @PostMapping("/{id}/rechazar") @PreAuthorize("hasAnyRole('ADMIN', 'MULTIFUNCIONAL')") public ResponseEntity<CotizacionResponseDTO> rechazar(@PathVariable UUID id, @Valid @RequestBody RechazarCotizacionRequestDTO request) { return ResponseEntity.ok(cotizacionService.rechazar(id, request)); }
    @PostMapping("/{id}/no-comprada") @PreAuthorize("hasAnyRole('ADMIN', 'MULTIFUNCIONAL')") public ResponseEntity<CotizacionResponseDTO> noComprada(@PathVariable UUID id) { return ResponseEntity.ok(cotizacionService.marcarNoComprada(id)); }
    @GetMapping("/{id}/documento") public ResponseEntity<byte[]> documento(@PathVariable UUID id, @RequestParam(defaultValue = "pdf") String formato) { return cotizacionDocumento.descargar(id, formato); }
    /** Converts an accepted quotation to the protected POS flow. */
    @Operation(summary = "Convertir en venta", description = "ACEPTADA → CONVERTIDA: registra la venta con el turno de caja abierto del usuario (mismas reglas que POST /ventas).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Cotización, almacén o turno inexistente"),
            @ApiResponse(responseCode = "409", description = "La cotización no está ACEPTADA, venció o no hay turno abierto")
    })
    @PostMapping("/{id}/convertir") @PreAuthorize("hasAnyRole('ADMIN', 'MULTIFUNCIONAL')") public ResponseEntity<VentaProcesadaResponseDTO> convertir(@PathVariable UUID id, @Valid @RequestBody ConvertirCotizacionRequestDTO request) { return ResponseEntity.ok(cotizacionService.convertir(id, request)); }
    /** Lists and transitions expired issued quotations. */
    @Operation(summary = "Cotizaciones vencidas", description = "Marca como vencidas las EMITIDAS cuya vigencia pasó y las devuelve.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping("/vencidas") public ResponseEntity<List<CotizacionListadoDTO>> vencidas() { return ResponseEntity.ok(cotizacionService.vencidas()); }
    /** Returns commercial dashboard counters. */
    @Operation(summary = "Dashboard de cotizaciones", description = "Contadores comerciales por estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping("/dashboard") public ResponseEntity<Map<String,Object>> dashboard() { return ResponseEntity.ok(cotizacionService.dashboard()); }
}
