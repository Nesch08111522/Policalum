package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.infrastructure.repository.projection.EfectivoPorTurnoProjection;
import com.policalum.erp.infrastructure.repository.projection.PagoPorVentaProjection;
import com.policalum.erp.infrastructure.repository.projection.MontoPorMetodoProjection;
import com.policalum.erp.domain.enums.TipoComprobante;
import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.infrastructure.repository.projection.MontoPorSedeMetodoProjection;
import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.model.PagoVenta;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoVentaRepository extends JpaRepository<PagoVenta, UUID> {
    @Query("select coalesce(sum(p.monto), 0) from PagoVenta p where p.venta.turnoCaja.id = :turnoId and p.metodoPago = :metodo")
    BigDecimal sumMontoByTurnoAndMetodo(@Param("turnoId") UUID turnoId, @Param("metodo") MetodoPago metodo);
    List<PagoVenta> findByVenta_Id(UUID ventaId);

    // ============ Módulo 6: flujo de caja ============

    /**
     * Cobros de ventas en [desde, hasta) por sede y método. Se usa la fecha de la venta:
     * los pagos se registran en el mismo momento que la venta.
     */
    @Query("SELECT p.venta.sede.id AS sedeId, p.metodoPago AS metodo, COALESCE(SUM(p.monto), 0) AS total " +
            "FROM PagoVenta p WHERE p.venta.estado = :estado AND p.venta.createdAt >= :desde AND p.venta.createdAt < :hasta AND " +
            "(:sedeId IS NULL OR p.venta.sede.id = :sedeId) GROUP BY p.venta.sede.id, p.metodoPago")
    List<MontoPorSedeMetodoProjection> cobrosPorSedeYMetodo(@Param("estado") com.policalum.erp.domain.enums.EstadoVenta estado,
                                                           @Param("desde") java.time.LocalDateTime desde,
                                                           @Param("hasta") java.time.LocalDateTime hasta,
                                                           @Param("sedeId") UUID sedeId);

    // ============ Módulo 12: reportes ============

    /** Cobros por método de las ventas que cumplen los filtros del reporte de ventas. */
    @Query("SELECT p.metodoPago AS metodo, COUNT(p) AS cantidad, COALESCE(SUM(p.monto), 0) AS total " +
            "FROM PagoVenta p JOIN p.venta v WHERE " +
            "v.estado = :estado AND v.createdAt >= :desde AND v.createdAt < :hasta AND (:sedeId IS NULL OR " +
            "v.sede.id = :sedeId) AND (:vendedorId IS NULL OR v.user.id = :vendedorId) AND (:clienteId IS NULL OR " +
            "v.cliente.id = :clienteId) AND (:canal IS NULL OR v.canalVenta = :canal) AND (:tipo IS NULL OR " +
            "v.tipoComprobante = :tipo)" +
            " GROUP BY p.metodoPago")
    List<MontoPorMetodoProjection> pagosPorMetodo(@Param("estado") com.policalum.erp.domain.enums.EstadoVenta estado,
            @Param("desde") java.time.LocalDateTime desde, @Param("hasta") java.time.LocalDateTime hasta,
            @Param("sedeId") UUID sedeId, @Param("vendedorId") UUID vendedorId, @Param("clienteId") UUID clienteId,
            @Param("canal") CanalVenta canal, @Param("tipo") TipoComprobante tipo);

    /** Pagos de un lote de ventas, sumados por venta y método. */
    @Query("SELECT p.venta.id AS ventaId, p.metodoPago AS metodo, COALESCE(SUM(p.monto), 0) AS total " +
            "FROM PagoVenta p WHERE p.venta.id IN :ventaIds GROUP BY p.venta.id, p.metodoPago")
    List<PagoPorVentaProjection> pagosPorVentas(@Param("ventaIds") java.util.Collection<UUID> ventaIds);

    /**
     * Efectivo cobrado por turno para un lote de turnos. Igual que sumMontoByTurnoAndMetodo (que usa el
     * cierre de caja) incluye ventas anuladas; anuladas se separa para el arqueo.
     */
    @Query("SELECT v.turnoCaja.id AS turnoId, COALESCE(SUM(p.monto), 0) AS total, " +
            "COALESCE(SUM(CASE WHEN v.estado = com.policalum.erp.domain.enums.EstadoVenta.ANULADA THEN p.monto ELSE 0 END), 0) AS anuladas " +
            "FROM PagoVenta p JOIN p.venta v WHERE p.metodoPago = com.policalum.erp.domain.enums.MetodoPago.EFECTIVO " +
            "AND v.turnoCaja.id IN :turnoIds GROUP BY v.turnoCaja.id")
    List<EfectivoPorTurnoProjection> efectivoPorTurnos(@Param("turnoIds") java.util.Collection<UUID> turnoIds);
}
