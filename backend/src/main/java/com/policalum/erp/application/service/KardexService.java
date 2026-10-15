package com.policalum.erp.application.service;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KardexService {

    /**
     * Registra un movimiento y actualiza el stock (con bloqueo de la fila de stock).
     * Uso interno de ventas, compras y devoluciones.
     */
    KardexMovimientoResponseDTO registrarMovimiento(KardexMovimientoRequestDTO dto);

    /** Transferencia entre almacenes: SALIDA en origen + ENTRADA en destino. */
    List<KardexMovimientoResponseDTO> registrarTransferencia(TransferenciaRequestDTO dto);

    /**
     * Igual que {@link #registrarTransferencia(TransferenciaRequestDTO)} pero ambos movimientos
     * quedan con referenciaId (Módulo 11: id de la TransferenciaStock que los agrupa).
     */
    List<KardexMovimientoResponseDTO> registrarTransferencia(TransferenciaRequestDTO dto, UUID referenciaId);

    /** Búsqueda original (GET /api/v1/kardex). Para consultas nuevas usar {@link #buscarMovimientos}. */
    Page<KardexMovimientoResponseDTO> searchMovimientos(UUID productoId, UUID almacenId,
                                                        LocalDateTime fechaInicio, LocalDateTime fechaFin, Pageable pageable);

    // ============ Módulo 10: consultas ============

    /** Movimientos filtrados, más recientes primero. Fechas inclusivas; sin fechas = todo el historial. */
    Page<KardexMovimientoDetalleDTO> buscarMovimientos(UUID productoId, UUID almacenId, TipoReferencia tipoReferencia,
                                                       LocalDate desde, LocalDate hasta, Pageable pageable);

    /** Detalle de un movimiento. 404 si no existe. */
    KardexMovimientoDetalleDTO obtenerMovimiento(UUID id);

    /** Saldos por producto/almacén (stock > 0 o con movimientos). busqueda: nombre o SKU. */
    Page<SaldoKardexDTO> saldos(UUID almacenId, UUID sedeId, String busqueda, boolean soloBajoMinimo, Pageable pageable);

    /** Saldos de un producto en todos los almacenes. 404 si el producto no existe. */
    SaldosProductoDTO saldosPorProducto(UUID productoId);

    /** Saldos de un almacén. 404 si el almacén no existe. */
    Page<SaldoKardexDTO> saldosPorAlmacen(UUID almacenId, String busqueda, boolean soloBajoMinimo, Pageable pageable);

    /** Inventario valorizado a costo promedio, consolidado por producto. Solo ADMIN. */
    ValorizacionKardexDTO valorizacion(UUID sedeId);

    /** Inventario valorizado de un almacén. Solo ADMIN. */
    ValorizacionKardexDTO valorizacionAlmacen(UUID almacenId);

    /** Reporte de mermas (resumen por producto + detalle paginado). Solo ADMIN. */
    ReporteMermasDTO mermas(UUID productoId, UUID almacenId, LocalDate desde, LocalDate hasta, Pageable pageable);

    /** Historial de un producto con saldos actuales y stock resultante de cada movimiento. */
    HistorialProductoDTO historialProducto(UUID productoId, UUID almacenId, LocalDate desde, LocalDate hasta, Pageable pageable);

    /** CSV de movimientos (UTF-8 con BOM). 400 si supera el máximo exportable. Solo ADMIN; queda auditado. */
    ArchivoDescargaDTO exportarCsv(UUID productoId, UUID almacenId, TipoReferencia tipoReferencia, LocalDate desde, LocalDate hasta);
}
