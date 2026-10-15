package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.AjusteStockRequestDTO;
import com.policalum.erp.application.dto.request.TransferenciaStockRequestDTO;
import com.policalum.erp.application.dto.response.AjusteStockResponseDTO;
import com.policalum.erp.application.dto.response.SaldoKardexDTO;
import com.policalum.erp.application.dto.response.StockConsolidadoDTO;
import com.policalum.erp.application.dto.response.StockDashboardDTO;
import com.policalum.erp.application.dto.response.TransferenciaStockResponseDTO;
import com.policalum.erp.application.service.StockOperacionService;
import com.policalum.erp.domain.enums.TipoMovimiento;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

/**
 * Ajustes, transferencias y consultas de stock (Módulo 11). ADMIN y MULTIFUNCIONAL;
 * MULTIFUNCIONAL queda limitado a su sede. Fechas inclusivas (yyyy-MM-dd).
 */
@Tag(name = "Inventario")
@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
public class StockOperacionController {

    private final StockOperacionService service;

    /** Ajuste de inventario: cantidad positiva = sobrante, negativa = faltante. 201 + Location del histórico. */
    @PostMapping("/ajuste")
    public ResponseEntity<AjusteStockResponseDTO> registrarAjuste(@Valid @RequestBody AjusteStockRequestDTO request) {
        AjusteStockResponseDTO ajuste = service.registrarAjuste(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/stock/ajustes").queryParam("productoId", ajuste.productoId()).build().toUri();
        return ResponseEntity.created(location).body(ajuste);
    }

    /** Histórico de ajustes. */
    @GetMapping("/ajustes")
    public ResponseEntity<Page<AjusteStockResponseDTO>> ajustes(
            @RequestParam(required = false) UUID productoId,
            @RequestParam(required = false) UUID almacenId,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.buscarAjustes(productoId, almacenId, sedeId, userId, tipo, desde, hasta, pageable));
    }

    /** Transferencia atómica entre almacenes. 201 + Location del detalle. */
    @PostMapping("/transferencia")
    public ResponseEntity<TransferenciaStockResponseDTO> transferir(@Valid @RequestBody TransferenciaStockRequestDTO request) {
        TransferenciaStockResponseDTO t = service.transferir(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/stock/transferencias/{id}").buildAndExpand(t.id()).toUri();
        return ResponseEntity.created(location).body(t);
    }

    /** Histórico de transferencias (almacenId / sedeId filtran por origen o destino). */
    @GetMapping("/transferencias")
    public ResponseEntity<Page<TransferenciaStockResponseDTO>> transferencias(
            @RequestParam(required = false) UUID productoId,
            @RequestParam(required = false) UUID almacenId,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(defaultValue = "false") boolean soloEntreSedes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.buscarTransferencias(productoId, almacenId, sedeId, soloEntreSedes, desde, hasta, pageable));
    }

    /** Detalle de una transferencia con su salida y su entrada. */
    @GetMapping("/transferencias/{id}")
    public ResponseEntity<TransferenciaStockResponseDTO> transferencia(@PathVariable UUID id) {
        return ResponseEntity.ok(service.obtenerTransferencia(id));
    }

    /** Alertas de stock bajo el mínimo (incluye agotados). */
    @GetMapping("/bajo-stock")
    public ResponseEntity<Page<SaldoKardexDTO>> bajoStock(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) UUID almacenId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.bajoStock(sedeId, almacenId, pageable));
    }

    /** Stock total por producto sumando todos los almacenes. */
    @GetMapping("/consolidado")
    public ResponseEntity<Page<StockConsolidadoDTO>> consolidado(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) String busqueda,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.consolidado(sedeId, busqueda, pageable));
    }

    /** KPIs de inventario del día. */
    @GetMapping("/dashboard")
    public ResponseEntity<StockDashboardDTO> dashboard(@RequestParam(required = false) UUID sedeId) {
        return ResponseEntity.ok(service.dashboard(sedeId));
    }
}
