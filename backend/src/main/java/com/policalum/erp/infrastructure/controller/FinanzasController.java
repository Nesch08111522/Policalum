package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.response.EvolucionMensualResponseDTO;
import com.policalum.erp.application.dto.response.FlujoCajaRealResponseDTO;
import com.policalum.erp.application.dto.response.FlujoDevengadoResponseDTO;
import com.policalum.erp.application.dto.response.KpisFinancierosResponseDTO;
import com.policalum.erp.application.dto.response.PatrimonioResponseDTO;
import com.policalum.erp.application.dto.response.ProyeccionFlujoResponseDTO;
import com.policalum.erp.application.service.DashboardFinancieroService;
import com.policalum.erp.application.service.FlujoCajaService;
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

/**
 * Flujo de caja y dashboards financieros (Módulo 6). Solo ADMIN.
 * En todos los endpoints: sin sedeId = consolidado de la empresa (+ desglose por sede donde aplica).
 */
@Tag(name = "Finanzas")
@RestController
@RequestMapping("/api/v1/finanzas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FinanzasController {

    private final FlujoCajaService flujoCajaService;
    private final DashboardFinancieroService dashboardService;

    /** Flujo de caja real (efectivo y digital). Fechas inclusivas; sin fechas: mes en curso. */
    @GetMapping("/flujo-caja")
    public ResponseEntity<FlujoCajaRealResponseDTO> flujoReal(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(flujoCajaService.flujoReal(sedeId, desde, hasta));
    }

    /** Vista devengada: utilidad neta consolidada y por sede. */
    @GetMapping("/flujo-caja/devengado")
    public ResponseEntity<FlujoDevengadoResponseDTO> devengado(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(flujoCajaService.devengado(sedeId, desde, hasta));
    }

    /** Proyección de 1 a 12 meses (por defecto 3), empezando por el mes en curso. */
    @GetMapping("/flujo-caja/proyeccion")
    public ResponseEntity<ProyeccionFlujoResponseDTO> proyeccion(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) Integer meses) {
        return ResponseEntity.ok(flujoCajaService.proyeccion(sedeId, meses));
    }

    /** Activos, pasivos, patrimonio neto y separación dueño/empresa, a hoy. */
    @GetMapping("/dashboard/patrimonio")
    public ResponseEntity<PatrimonioResponseDTO> patrimonio(@RequestParam(required = false) UUID sedeId) {
        return ResponseEntity.ok(dashboardService.patrimonio(sedeId));
    }

    /** KPIs del periodo comparados con el periodo anterior de igual duración. */
    @GetMapping("/dashboard/kpis")
    public ResponseEntity<KpisFinancierosResponseDTO> kpis(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(dashboardService.kpis(sedeId, desde, hasta));
    }

    /** Serie mensual de 1 a 24 meses (por defecto 12), incluido el mes en curso. */
    @GetMapping("/dashboard/evolucion-mensual")
    public ResponseEntity<EvolucionMensualResponseDTO> evolucionMensual(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) Integer meses) {
        return ResponseEntity.ok(dashboardService.evolucionMensual(sedeId, meses));
    }
}
