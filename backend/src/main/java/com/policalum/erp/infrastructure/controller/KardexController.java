package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.policalum.erp.application.dto.request.KardexMovimientoRequestDTO;
import com.policalum.erp.application.dto.request.TransferenciaRequestDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.HistorialProductoDTO;
import com.policalum.erp.application.dto.response.KardexMovimientoDetalleDTO;
import com.policalum.erp.application.dto.response.KardexMovimientoResponseDTO;
import com.policalum.erp.application.dto.response.ReporteMermasDTO;
import com.policalum.erp.application.dto.response.SaldoKardexDTO;
import com.policalum.erp.application.dto.response.SaldosProductoDTO;
import com.policalum.erp.application.dto.response.ValorizacionKardexDTO;
import com.policalum.erp.domain.enums.TipoReferencia;
import com.policalum.erp.application.service.KardexService;

import jakarta.validation.Valid;

@Tag(name = "Inventario")
@RestController
@RequestMapping("/api/v1/kardex")
public class KardexController {

    private final KardexService kardexService;

    public KardexController(KardexService kardexService) {
        this.kardexService = kardexService;
    }

    @PostMapping("/movimientos")
    public ResponseEntity<KardexMovimientoResponseDTO> registrarMovimiento(
            @Valid @RequestBody KardexMovimientoRequestDTO dto) {
        KardexMovimientoResponseDTO response = kardexService.registrarMovimiento(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transferencias")
    public ResponseEntity<List<KardexMovimientoResponseDTO>> registrarTransferencia(
            @Valid @RequestBody TransferenciaRequestDTO dto) {
        List<KardexMovimientoResponseDTO> response = kardexService.registrarTransferencia(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<KardexMovimientoResponseDTO>> searchMovimientos(
            @RequestParam(required = false) UUID productoId,
            @RequestParam(required = false) UUID almacenId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<KardexMovimientoResponseDTO> response = kardexService.searchMovimientos(
                productoId, almacenId, fechaInicio, fechaFin, pageable);
        return ResponseEntity.ok(response);
    }

    // ===================== Módulo 10: consultas =====================
    // Fechas inclusivas (yyyy-MM-dd); sin fechas = todo el historial.
    // Saldos, historial y detalle: ADMIN y MULTIFUNCIONAL (sin costos).
    // Valorización, mermas y exportación: solo ADMIN (muestran costos).

    /** Movimientos paginados con datos planos (más recientes primero). */
    @GetMapping("/movimientos")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<Page<KardexMovimientoDetalleDTO>> buscarMovimientos(
            @RequestParam(required = false) UUID productoId,
            @RequestParam(required = false) UUID almacenId,
            @RequestParam(required = false) TipoReferencia tipoReferencia,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(kardexService.buscarMovimientos(productoId, almacenId, tipoReferencia, desde, hasta, pageable));
    }

    /** Detalle de un movimiento. */
    @GetMapping("/movimientos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<KardexMovimientoDetalleDTO> obtenerMovimiento(@PathVariable UUID id) {
        return ResponseEntity.ok(kardexService.obtenerMovimiento(id));
    }

    /** Saldos por producto/almacén (stock > 0 o con movimientos). busqueda: nombre o SKU. */
    @GetMapping("/saldos")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<Page<SaldoKardexDTO>> saldos(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "false") boolean soloBajoMinimo,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(kardexService.saldos(null, sedeId, busqueda, soloBajoMinimo, pageable));
    }

    /** Saldos de un producto en todos los almacenes, con el total. */
    @GetMapping("/saldos/producto/{productoId}")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<SaldosProductoDTO> saldosPorProducto(@PathVariable UUID productoId) {
        return ResponseEntity.ok(kardexService.saldosPorProducto(productoId));
    }

    /** Saldos de un almacén. */
    @GetMapping("/saldos/almacen/{almacenId}")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<Page<SaldoKardexDTO>> saldosPorAlmacen(
            @PathVariable UUID almacenId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "false") boolean soloBajoMinimo,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(kardexService.saldosPorAlmacen(almacenId, busqueda, soloBajoMinimo, pageable));
    }

    /** Inventario valorizado a costo promedio, consolidado por producto (sedeId opcional). */
    @GetMapping("/valorizacion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ValorizacionKardexDTO> valorizacion(@RequestParam(required = false) UUID sedeId) {
        return ResponseEntity.ok(kardexService.valorizacion(sedeId));
    }

    /** Inventario valorizado de un almacén. */
    @GetMapping("/valorizacion/almacen/{almacenId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ValorizacionKardexDTO> valorizacionAlmacen(@PathVariable UUID almacenId) {
        return ResponseEntity.ok(kardexService.valorizacionAlmacen(almacenId));
    }

    /** Reporte de mermas: resumen por producto + detalle paginado. */
    @GetMapping("/mermas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReporteMermasDTO> mermas(
            @RequestParam(required = false) UUID productoId,
            @RequestParam(required = false) UUID almacenId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(kardexService.mermas(productoId, almacenId, desde, hasta, pageable));
    }

    /** Historial de un producto: saldos actuales + movimientos con stock resultante. */
    @GetMapping("/producto/{productoId}/historial")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<HistorialProductoDTO> historialProducto(
            @PathVariable UUID productoId,
            @RequestParam(required = false) UUID almacenId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(kardexService.historialProducto(productoId, almacenId, desde, hasta, pageable));
    }

    /** CSV de movimientos (UTF-8 con BOM, máximo 50 000 filas). Queda auditado. */
    @GetMapping("/exportar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> exportar(
            @RequestParam(required = false) UUID productoId,
            @RequestParam(required = false) UUID almacenId,
            @RequestParam(required = false) TipoReferencia tipoReferencia,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        ArchivoDescargaDTO archivo = kardexService.exportarCsv(productoId, almacenId, tipoReferencia, desde, hasta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archivo.nombreOriginal()).build().toString())
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .body(archivo.resource());
    }
}
