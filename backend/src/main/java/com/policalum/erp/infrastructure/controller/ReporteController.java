package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.FiltroVentasReporteDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.ArqueosReporteDTO;
import com.policalum.erp.application.dto.response.ClientesFrecuentesDTO;
import com.policalum.erp.application.dto.response.ProductosEstrellaDTO;
import com.policalum.erp.application.dto.response.ProveedoresRankingDTO;
import com.policalum.erp.application.dto.response.RentabilidadReporteDTO;
import com.policalum.erp.application.dto.response.VentaReporteDTO;
import com.policalum.erp.application.dto.response.VentasResumenDTO;
import com.policalum.erp.application.service.ReporteService;
import com.policalum.erp.domain.enums.AgrupacionReporte;
import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.domain.enums.CriterioRankingCliente;
import com.policalum.erp.domain.enums.CriterioRankingProducto;
import com.policalum.erp.domain.enums.CriterioRankingProveedor;
import com.policalum.erp.domain.enums.EstadoTurno;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.domain.enums.TipoComprobante;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reportes consolidados (Módulo 12). Consulta: ADMIN y MULTIFUNCIONAL (este último solo su sede y sin
 * costos). Exportación CSV: solo ADMIN. Sin fechas = mes en curso; desde/hasta inclusivos (yyyy-MM-dd).
 */
@Tag(name = "Reportes")
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
public class ReporteController {

    private final ReporteService service;

    // ------------------------------------------------------------------ ventas

    /** Totales, ticket promedio, serie por DIA/SEMANA/MES, cobros por método y devoluciones. */
    @GetMapping("/ventas/resumen")
    public ResponseEntity<VentasResumenDTO> resumenVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) UUID vendedorId,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) CanalVenta canal,
            @RequestParam(required = false) TipoComprobante tipoComprobante,
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) AgrupacionReporte agrupacion) {
        return ResponseEntity.ok(service.resumenVentas(
                new FiltroVentasReporteDTO(desde, hasta, sedeId, vendedorId, clienteId, canal, tipoComprobante, estado), agrupacion));
    }

    /** Detalle de ventas paginado, más recientes primero. */
    @GetMapping("/ventas")
    public ResponseEntity<Page<VentaReporteDTO>> ventas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) UUID vendedorId,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) CanalVenta canal,
            @RequestParam(required = false) TipoComprobante tipoComprobante,
            @RequestParam(required = false) EstadoVenta estado,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.ventas(
                new FiltroVentasReporteDTO(desde, hasta, sedeId, vendedorId, clienteId, canal, tipoComprobante, estado), pageable));
    }

    /** CSV del detalle de ventas (tope: Configuración > reportes). */
    @GetMapping("/ventas/exportar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> exportarVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) UUID vendedorId,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) CanalVenta canal,
            @RequestParam(required = false) TipoComprobante tipoComprobante,
            @RequestParam(required = false) EstadoVenta estado) {
        return descarga(service.exportarVentas(
                new FiltroVentasReporteDTO(desde, hasta, sedeId, vendedorId, clienteId, canal, tipoComprobante, estado)));
    }

    // ------------------------------------------------------------------ productos

    /** Rentabilidad por producto y servicio (MULTIFUNCIONAL: sin costo ni margen). */
    @GetMapping("/productos/rentabilidad")
    public ResponseEntity<RentabilidadReporteDTO> rentabilidad(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) UUID categoriaId) {
        return ResponseEntity.ok(service.rentabilidad(desde, hasta, sedeId, categoriaId));
    }

    /** CSV de rentabilidad con costos. */
    @GetMapping("/productos/rentabilidad/exportar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> exportarRentabilidad(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) UUID categoriaId) {
        return descarga(service.exportarRentabilidad(desde, hasta, sedeId, categoriaId));
    }

    /** Top N de productos por CANTIDAD, INGRESO (default) o MARGEN (solo ADMIN). top 1..100, default 10. */
    @GetMapping("/productos/estrella")
    public ResponseEntity<ProductosEstrellaDTO> productosEstrella(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) CriterioRankingProducto criterio,
            @RequestParam(required = false) Integer top) {
        return ResponseEntity.ok(service.productosEstrella(desde, hasta, sedeId, categoriaId, criterio, top));
    }

    // ------------------------------------------------------------------ clientes

    /** Top N de clientes por CANTIDAD (default), MONTO o FRECUENCIA. */
    @GetMapping("/clientes/frecuentes")
    public ResponseEntity<ClientesFrecuentesDTO> clientesFrecuentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) CriterioRankingCliente criterio,
            @RequestParam(required = false) Integer top) {
        return ResponseEntity.ok(service.clientesFrecuentes(desde, hasta, sedeId, criterio, top));
    }

    /** CSV con todos los clientes del periodo. */
    @GetMapping("/clientes/frecuentes/exportar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> exportarClientes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) CriterioRankingCliente criterio) {
        return descarga(service.exportarClientes(desde, hasta, sedeId, criterio));
    }

    // ------------------------------------------------------------------ proveedores

    /** Top N de proveedores por FRECUENCIA (default), ENTREGA o VOLUMEN (solo ADMIN). */
    @GetMapping("/proveedores/ranking")
    public ResponseEntity<ProveedoresRankingDTO> rankingProveedores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) CriterioRankingProveedor criterio,
            @RequestParam(required = false) Integer top) {
        return ResponseEntity.ok(service.rankingProveedores(desde, hasta, sedeId, criterio, top));
    }

    /** CSV con todos los proveedores del periodo. */
    @GetMapping("/proveedores/ranking/exportar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> exportarProveedores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) CriterioRankingProveedor criterio) {
        return descarga(service.exportarProveedores(desde, hasta, sedeId, criterio));
    }

    // ------------------------------------------------------------------ caja

    /** Arqueo por turno (fecha de apertura) con resumen de faltantes/sobrantes. MULTIFUNCIONAL: sus turnos. */
    @GetMapping("/caja/arqueos")
    public ResponseEntity<ArqueosReporteDTO> arqueos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) EstadoTurno estado,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.arqueos(desde, hasta, sedeId, usuarioId, estado, pageable));
    }

    /** CSV de arqueos. */
    @GetMapping("/caja/arqueos/exportar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> exportarArqueos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) EstadoTurno estado) {
        return descarga(service.exportarArqueos(desde, hasta, sedeId, usuarioId, estado));
    }

    private static ResponseEntity<Resource> descarga(ArchivoDescargaDTO archivo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archivo.nombreOriginal()).build().toString())
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .body(archivo.resource());
    }
}
