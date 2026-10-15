package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.ProcesarReembolsoRequestDTO;
import com.policalum.erp.application.dto.request.ReembolsoRequestDTO;
import com.policalum.erp.application.dto.response.ReembolsoResponseDTO;
import com.policalum.erp.application.service.ReembolsoService;
import com.policalum.erp.domain.enums.EstadoReembolso;
import com.policalum.erp.domain.enums.MetodoReembolso;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
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

/** Reembolsos. El personal los registra y consulta; procesarlos (pagar) es solo ADMIN. */
@Tag(name = "Devoluciones")
@RestController
@RequestMapping("/api/v1/reembolsos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
public class ReembolsoController {

    private final ReembolsoService reembolsoService;

    /** Registra el reembolso PENDIENTE de una devolución aprobada. 201 + Location. */
    @PostMapping
    public ResponseEntity<ReembolsoResponseDTO> crear(@Valid @RequestBody ReembolsoRequestDTO request) {
        ReembolsoResponseDTO creado = reembolsoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    /** Listado paginado; fechas inclusivas sobre la fecha de registro. */
    @GetMapping
    public ResponseEntity<Page<ReembolsoResponseDTO>> buscar(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) EstadoReembolso estado,
            @RequestParam(required = false) MetodoReembolso metodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reembolsoService.buscar(sedeId, estado, metodo, desde, hasta, pageable));
    }

    /** Detalle. */
    @GetMapping("/{id}")
    public ResponseEntity<ReembolsoResponseDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(reembolsoService.obtener(id));
    }

    /** Marca PROCESADO; si es EFECTIVO sale del turno de caja abierto del admin. Cuerpo opcional. */
    @PostMapping("/{id}/procesar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReembolsoResponseDTO> procesar(@PathVariable UUID id,
                                                         @Valid @RequestBody(required = false) ProcesarReembolsoRequestDTO request) {
        return ResponseEntity.ok(reembolsoService.procesar(id, request));
    }
}
