package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.PagoCxPRequestDTO;
import com.policalum.erp.application.dto.response.CuentaPorPagarResponseDTO;
import com.policalum.erp.application.dto.response.ResumenCxPDashboardDTO;
import com.policalum.erp.application.service.CuentaPorPagarService;
import com.policalum.erp.domain.enums.EstadoCxP;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Cuentas por pagar")
@RestController
@RequestMapping("/api/v1/cuentas-por-pagar")
@RequiredArgsConstructor
public class CuentaPorPagarController {

    private final CuentaPorPagarService cuentaPorPagarService;

    @GetMapping
    public ResponseEntity<Page<CuentaPorPagarResponseDTO>> buscar(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) UUID proveedorId,
            @RequestParam(required = false) EstadoCxP estado,
            @RequestParam(required = false) Boolean soloVencidas,
            @PageableDefault(size = 20, sort = "fechaVencimiento", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(cuentaPorPagarService.buscar(sedeId, proveedorId, estado, soloVencidas, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaPorPagarResponseDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(cuentaPorPagarService.obtener(id));
    }

    @PostMapping("/{id}/pago")
    public ResponseEntity<CuentaPorPagarResponseDTO> registrarPago(
            @PathVariable UUID id, @Valid @RequestBody PagoCxPRequestDTO dto) {
        return ResponseEntity.ok(cuentaPorPagarService.registrarPago(id, dto));
    }

    @GetMapping("/compra/{compraId}")
    public ResponseEntity<CuentaPorPagarResponseDTO> obtenerPorCompra(@PathVariable UUID compraId) {
        return ResponseEntity.ok(cuentaPorPagarService.obtenerPorCompra(compraId));
    }

    @GetMapping("/vencidas")
    public ResponseEntity<List<CuentaPorPagarResponseDTO>> vencidas(
            @RequestParam(required = false) UUID sedeId) {
        return ResponseEntity.ok(cuentaPorPagarService.vencidas(sedeId));
    }

    @GetMapping("/proximas-vencer")
    public ResponseEntity<List<CuentaPorPagarResponseDTO>> proximasAVencer(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(defaultValue = "7") int dias) {
        return ResponseEntity.ok(cuentaPorPagarService.proximasAVencer(sedeId, dias));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ResumenCxPDashboardDTO> dashboard(
            @RequestParam(required = false) UUID sedeId) {
        return ResponseEntity.ok(cuentaPorPagarService.dashboard(sedeId));
    }
}