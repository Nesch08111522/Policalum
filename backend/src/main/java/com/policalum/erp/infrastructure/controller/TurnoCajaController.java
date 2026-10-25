package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.AperturaCajaRequestDTO;
import com.policalum.erp.application.dto.request.CierreCajaRequestDTO;
import com.policalum.erp.application.dto.request.CajaChicaRequestDTO;
import com.policalum.erp.application.dto.response.CajaChicaResponseDTO;
import com.policalum.erp.application.dto.response.ResumenCierreCajaDTO;
import com.policalum.erp.application.dto.response.TurnoAbiertoResponseDTO;
import com.policalum.erp.application.dto.response.TurnoCajaResponseDTO;
import com.policalum.erp.application.service.serviceImpl.CajaChicaServiceImpl;
import java.util.List;
import com.policalum.erp.application.service.TurnoCajaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Caja")
@RestController
@RequestMapping("/api/v1/turnos-caja")
@RequiredArgsConstructor
public class TurnoCajaController {
    private final TurnoCajaService turnoService;
    private final CajaChicaServiceImpl cajaChicaService;
    @GetMapping("/abierto") public ResponseEntity<TurnoAbiertoResponseDTO> abierto() { return ResponseEntity.ok(turnoService.turnoAbierto()); }
    @GetMapping("/{id}/caja-chica") public ResponseEntity<List<CajaChicaResponseDTO>> listarCajaChica(@PathVariable UUID id) { return ResponseEntity.ok(cajaChicaService.listar(id)); }
    @PostMapping("/{id}/caja-chica") public ResponseEntity<CajaChicaResponseDTO> registrarCajaChica(@PathVariable UUID id, @Valid @RequestBody CajaChicaRequestDTO request) { return ResponseEntity.ok(cajaChicaService.registrar(id, request)); }
    @PostMapping
    public ResponseEntity<TurnoCajaResponseDTO> abrir(@Valid @RequestBody AperturaCajaRequestDTO request) {
        TurnoCajaResponseDTO turno = turnoService.abrir(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(turno.id()).toUri();
        return ResponseEntity.created(location).body(turno);
    }
    @GetMapping("/{id}/resumen") public ResponseEntity<ResumenCierreCajaDTO> resumen(@PathVariable UUID id) { return ResponseEntity.ok(turnoService.resumen(id)); }
    @PostMapping("/{id}/cierre") public ResponseEntity<ResumenCierreCajaDTO> cerrar(@PathVariable UUID id, @Valid @RequestBody CierreCajaRequestDTO request) { return ResponseEntity.ok(turnoService.cerrar(id, request)); }
}
