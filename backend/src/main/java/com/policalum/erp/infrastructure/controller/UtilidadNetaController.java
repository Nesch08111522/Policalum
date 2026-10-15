package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.response.UtilidadNetaResponseDTO;
import com.policalum.erp.application.service.UtilidadNetaService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Utilidad neta real (control financiero, solo ROLE_ADMIN). */
@Tag(name = "Finanzas")
@RestController
@RequestMapping("/api/v1/finanzas/utilidad-neta")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UtilidadNetaController {

    private final UtilidadNetaService utilidadNetaService;

    /** Utilidad por rango de fechas (inclusivo). Sin fechas: mes en curso. */
    @GetMapping
    public ResponseEntity<UtilidadNetaResponseDTO> calcular(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(utilidadNetaService.calcular(sedeId, desde, hasta));
    }

    /** Utilidad de un mes. Ej: /periodo?periodo=2026-10 */
    @GetMapping("/periodo")
    public ResponseEntity<UtilidadNetaResponseDTO> calcularPorPeriodo(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam String periodo) {
        return ResponseEntity.ok(utilidadNetaService.calcularPorPeriodo(sedeId, periodo));
    }
}
