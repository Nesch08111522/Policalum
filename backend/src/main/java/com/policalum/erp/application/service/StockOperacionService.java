package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.AjusteStockRequestDTO;
import com.policalum.erp.application.dto.request.TransferenciaStockRequestDTO;
import com.policalum.erp.application.dto.response.AjusteStockResponseDTO;
import com.policalum.erp.application.dto.response.SaldoKardexDTO;
import com.policalum.erp.application.dto.response.StockConsolidadoDTO;
import com.policalum.erp.application.dto.response.StockDashboardDTO;
import com.policalum.erp.application.dto.response.TransferenciaStockResponseDTO;
import com.policalum.erp.domain.enums.TipoMovimiento;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Ajustes, transferencias y consultas de stock (Módulo 11). ADMIN y MULTIFUNCIONAL.
 * MULTIFUNCIONAL solo opera y consulta almacenes de su sede; ADMIN, todas.
 * Fechas inclusivas [desde, hasta]; sin fechas = todo el historial.
 */
public interface StockOperacionService {

    /**
     * Ajuste de inventario (Kardex AJUSTE). cantidad > 0 = sobrante, < 0 = faltante.
     * 400 si deja stock negativo, si la cantidad es 0, o si vale más de S/ 500 sin justificación
     * de al menos 30 caracteres.
     */
    AjusteStockResponseDTO registrarAjuste(AjusteStockRequestDTO request);

    /** Histórico de ajustes, más recientes primero. */
    Page<AjusteStockResponseDTO> buscarAjustes(UUID productoId, UUID almacenId, UUID sedeId, UUID userId, TipoMovimiento tipo,
                                               LocalDate desde, LocalDate hasta, Pageable pageable);

    /** Transferencia atómica: SALIDA en origen + ENTRADA en destino. 400 si no hay stock suficiente. */
    TransferenciaStockResponseDTO transferir(TransferenciaStockRequestDTO request);

    /** Histórico de transferencias (almacenId/sedeId filtran por origen o destino). */
    Page<TransferenciaStockResponseDTO> buscarTransferencias(UUID productoId, UUID almacenId, UUID sedeId, boolean soloEntreSedes,
                                                             LocalDate desde, LocalDate hasta, Pageable pageable);

    /** Detalle de una transferencia con sus dos movimientos. 404 si no existe. */
    TransferenciaStockResponseDTO obtenerTransferencia(UUID id);

    /** Saldos con mínimo configurado y cantidad <= mínimo, por sede o almacén. */
    Page<SaldoKardexDTO> bajoStock(UUID sedeId, UUID almacenId, Pageable pageable);

    /** Stock total por producto sumando todos los almacenes (de la sede, si se indica). */
    Page<StockConsolidadoDTO> consolidado(UUID sedeId, String busqueda, Pageable pageable);

    /** KPIs de inventario del día. */
    StockDashboardDTO dashboard(UUID sedeId);
}
