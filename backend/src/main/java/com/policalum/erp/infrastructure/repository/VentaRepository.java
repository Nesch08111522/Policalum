package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.infrastructure.repository.projection.VentasPorClienteProjection;
import com.policalum.erp.infrastructure.repository.projection.VentasPorDiaProjection;
import com.policalum.erp.domain.enums.TipoComprobante;
import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.domain.model.Venta;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.infrastructure.repository.projection.TotalesFinancierosProjection;
import com.policalum.erp.infrastructure.repository.projection.VentasPorVendedorProjection;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VentaRepository extends JpaRepository<Venta, UUID> {

    @Query("SELECT u.nombre, u.apellido, COUNT(v), COALESCE(SUM(v.total), 0) " +
            "FROM Venta v JOIN v.user u WHERE v.estado = com.policalum.erp.domain.enums.EstadoVenta.COMPLETADA " +
            "AND v.createdAt >= :desde AND v.createdAt < :hasta " +
            "GROUP BY u.id, u.nombre, u.apellido ORDER BY SUM(v.total) DESC")
    java.util.List<Object[]> rendimientoVendedores(@Param("desde") java.time.LocalDateTime desde,
                                                    @Param("hasta") java.time.LocalDateTime hasta,
                                                    org.springframework.data.domain.Pageable pageable);

    /** Módulo 17: FOR UPDATE para que devoluciones/anulación concurrentes no se pisen. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venta v where v.id = :id")
    java.util.Optional<Venta> findByIdParaActualizar(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"sede", "user", "cliente", "turnoCaja"})
    @Query("select v from Venta v where (CAST(:sedeId AS string) IS NULL or v.sede.id = :sedeId) and (CAST(:userId AS string) IS NULL or v.user.id = :userId) and (CAST(:clienteId AS string) IS NULL or v.cliente.id = :clienteId) and (CAST(:estado AS string) IS NULL or v.estado = :estado) and (CAST(:canal AS string) IS NULL or v.canalVenta = :canal) and (CAST(:desde AS string) IS NULL or v.createdAt >= CAST(CAST(:desde AS string) AS timestamp)) and (CAST(:hasta AS string) IS NULL or v.createdAt <= CAST(CAST(:hasta AS string) AS timestamp))")
    Page<Venta> buscar(@Param("sedeId") UUID sedeId, @Param("userId") UUID userId, @Param("clienteId") UUID clienteId,
                       @Param("estado") EstadoVenta estado, @Param("canal") com.policalum.erp.domain.enums.CanalVenta canal,
                       @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, Pageable pageable);

    /** Totales de ventas en el rango [desde, hasta). Montos en PEN. */
    @Query("select coalesce(sum(v.subtotalNeto), 0) as neto, coalesce(sum(v.totalIgv), 0) as igv, " +
            "coalesce(sum(v.total), 0) as total, count(v) as cantidad from Venta v " +
            "where v.estado = :estado and v.createdAt >= CAST(:desde AS timestamp) and v.createdAt < CAST(:hasta AS timestamp) " +
            "and (CAST(:sedeId AS string) IS NULL or v.sede.id = :sedeId)")
    TotalesFinancierosProjection totalesPorPeriodo(@Param("estado") EstadoVenta estado,
                                                   @Param("desde") LocalDateTime desde,
                                                   @Param("hasta") LocalDateTime hasta,
                                                   @Param("sedeId") UUID sedeId);

    /** Módulo 9: ventas netas (sin IGV) por vendedor en [desde, hasta), todas las sedes. */
    @Query("select v.user.id as userId, coalesce(sum(v.subtotalNeto), 0) as neto, count(v) as cantidad " +
            "from Venta v where v.estado = :estado and v.createdAt >= CAST(:desde AS timestamp) and v.createdAt < CAST(:hasta AS timestamp) " +
            "group by v.user.id")
    List<VentasPorVendedorProjection> totalesPorVendedor(@Param("estado") EstadoVenta estado,
                                                         @Param("desde") LocalDateTime desde,
                                                         @Param("hasta") LocalDateTime hasta);

    // ============ Módulo 12: reportes ============

    /** Ventas por día calendario en [desde, hasta) con los filtros del reporte (null = sin filtro). */
    @Query("SELECT CAST(v.createdAt AS LocalDate) AS fecha, COUNT(v) AS cantidad, " +
            "COALESCE(SUM(v.subtotalNeto), 0) AS neto, COALESCE(SUM(v.totalIgv), 0) AS igv, COALESCE(SUM(v.total), 0) AS total " +
            "FROM Venta v WHERE " +
            "v.estado = :estado AND v.createdAt >= CAST(:desde AS timestamp) AND v.createdAt < CAST(:hasta AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR " +
            "v.sede.id = :sedeId) AND (CAST(:vendedorId AS string) IS NULL OR v.user.id = :vendedorId) AND (CAST(:clienteId AS string) IS NULL OR " +
            "v.cliente.id = :clienteId) AND (CAST(:canal AS string) IS NULL OR v.canalVenta = :canal) AND (CAST(:tipo AS string) IS NULL OR " +
            "v.tipoComprobante = :tipo)" +
            " GROUP BY CAST(v.createdAt AS LocalDate) ORDER BY CAST(v.createdAt AS LocalDate)")
    List<VentasPorDiaProjection> ventasPorDia(@Param("estado") EstadoVenta estado,
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
            @Param("sedeId") UUID sedeId, @Param("vendedorId") UUID vendedorId, @Param("clienteId") UUID clienteId,
            @Param("canal") CanalVenta canal, @Param("tipo") TipoComprobante tipo);

    /** Detalle de ventas del reporte, más recientes primero, con sede, vendedor y cliente cargados. */
    @Query(value = "SELECT v FROM Venta v JOIN FETCH v.sede JOIN FETCH v.user LEFT JOIN FETCH v.cliente WHERE " +
            "v.estado = :estado AND v.createdAt >= CAST(:desde AS timestamp) AND v.createdAt < CAST(:hasta AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR " +
            "v.sede.id = :sedeId) AND (CAST(:vendedorId AS string) IS NULL OR v.user.id = :vendedorId) AND (CAST(:clienteId AS string) IS NULL OR " +
            "v.cliente.id = :clienteId) AND (CAST(:canal AS string) IS NULL OR v.canalVenta = :canal) AND (CAST(:tipo AS string) IS NULL OR " +
            "v.tipoComprobante = :tipo)" +
            " ORDER BY v.createdAt DESC, v.id",
            countQuery = "SELECT COUNT(v) FROM Venta v WHERE " +
            "v.estado = :estado AND v.createdAt >= CAST(:desde AS timestamp) AND v.createdAt < CAST(:hasta AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR " +
            "v.sede.id = :sedeId) AND (CAST(:vendedorId AS string) IS NULL OR v.user.id = :vendedorId) AND (CAST(:clienteId AS string) IS NULL OR " +
            "v.cliente.id = :clienteId) AND (CAST(:canal AS string) IS NULL OR v.canalVenta = :canal) AND (CAST(:tipo AS string) IS NULL OR " +
            "v.tipoComprobante = :tipo)")
    Page<Venta> reporteVentas(@Param("estado") EstadoVenta estado,
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
            @Param("sedeId") UUID sedeId, @Param("vendedorId") UUID vendedorId, @Param("clienteId") UUID clienteId,
            @Param("canal") CanalVenta canal, @Param("tipo") TipoComprobante tipo, Pageable pageable);

    /**
     * Ventas COMPLETADAS por cliente en [desde, hasta). La fila con clienteId null agrupa las ventas sin
     * cliente (público general).
     */
    @Query("SELECT c.id AS clienteId, c.nombre AS nombre, c.tipoDocumento AS tipoDocumento, " +
            "c.numeroDocumento AS numeroDocumento, COUNT(v) AS cantidad, COALESCE(SUM(v.total), 0) AS total, " +
            "MIN(v.createdAt) AS primeraCompra, MAX(v.createdAt) AS ultimaCompra " +
            "FROM Venta v LEFT JOIN v.cliente c " +
            "WHERE v.estado = com.policalum.erp.domain.enums.EstadoVenta.COMPLETADA AND " +
            "v.createdAt >= CAST(:desde AS timestamp) AND v.createdAt < CAST(:hasta AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR v.sede.id = :sedeId) " +
            "GROUP BY c.id, c.nombre, c.tipoDocumento, c.numeroDocumento")
    List<VentasPorClienteProjection> ventasPorCliente(@Param("desde") LocalDateTime desde,
                                                      @Param("hasta") LocalDateTime hasta,
                                                      @Param("sedeId") UUID sedeId);

    @Query("select v.tipoComprobante, count(v) from Venta v where v.estado = com.policalum.erp.domain.enums.EstadoVenta.COMPLETADA and (CAST(:sedeId AS string) IS NULL or v.sede.id = :sedeId) group by v.tipoComprobante")
    List<Object[]> contarPorTipoComprobante(@Param("sedeId") UUID sedeId);

    @Query("select v.estadoSunat, count(v) from Venta v where (CAST(:sedeId AS string) IS NULL or v.sede.id = :sedeId) group by v.estadoSunat")
    List<Object[]> contarPorEstadoSunat(@Param("sedeId") UUID sedeId);

    @Query("select v from Venta v where (CAST(:sedeId AS string) IS NULL or v.sede.id = :sedeId) order by v.createdAt desc")
    List<Venta> recientesParaSunat(@Param("sedeId") UUID sedeId, Pageable pageable);
}
