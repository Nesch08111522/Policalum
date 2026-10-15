package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.AnularRetiroRequestDTO;
import com.policalum.erp.application.dto.request.RetiroSocioRequestDTO;
import com.policalum.erp.application.dto.response.RetiroSocioDashboardDTO;
import com.policalum.erp.application.dto.response.RetiroSocioListadoDTO;
import com.policalum.erp.application.dto.response.RetiroSocioResponseDTO;
import com.policalum.erp.application.service.RetiroSocioService;
import com.policalum.erp.domain.enums.EstadoRetiro;
import com.policalum.erp.domain.enums.OrigenFondos;
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

/** Retiros de socios. Todo el recurso es exclusivo de ROLE_ADMIN. */
@Tag(name = "Finanzas")
@RestController
@RequestMapping("/api/v1/finanzas/retiros-socios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FinanzasRetiroSocioController {

    private final RetiroSocioService retiroSocioService;

    /** Registra un retiro del admin autenticado. 201 + Location. */
    @PostMapping
    public ResponseEntity<RetiroSocioResponseDTO> registrar(@Valid @RequestBody RetiroSocioRequestDTO request) {
        RetiroSocioResponseDTO creado = retiroSocioService.registrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    /** Resumen de retiros del periodo (por defecto, mes en curso). */
    @GetMapping("/dashboard")
    public ResponseEntity<RetiroSocioDashboardDTO> dashboard(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(retiroSocioService.dashboard(sedeId, desde, hasta));
    }

    /** Detalle de un retiro. */
    @GetMapping("/{id}")
    public ResponseEntity<RetiroSocioResponseDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(retiroSocioService.obtener(id));
    }

    /** Listado paginado. Filtros opcionales: admin, sede, estado, origen y rango de fechas (inclusivo). */
    @GetMapping
    public ResponseEntity<Page<RetiroSocioListadoDTO>> buscar(
            @RequestParam(required = false) UUID adminId,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) EstadoRetiro estado,
            @RequestParam(required = false) OrigenFondos origen,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fechaRetiro", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(retiroSocioService.buscar(adminId, sedeId, estado, origen, desde, hasta, pageable));
    }

    /** Anula un retiro con motivo; revierte caja solo si el turno sigue abierto. */
    @PostMapping("/{id}/anular")
    public ResponseEntity<RetiroSocioResponseDTO> anular(@PathVariable UUID id,
                                                         @Valid @RequestBody AnularRetiroRequestDTO request) {
        return ResponseEntity.ok(retiroSocioService.anular(id, request));
    }
}
