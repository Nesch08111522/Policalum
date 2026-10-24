package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.infrastructure.repository.projection.MontoCantidadPorIdProjection;
import com.policalum.erp.infrastructure.repository.projection.MontoCantidadProjection;
import com.policalum.erp.domain.enums.EstadoDevolucion;
import com.policalum.erp.domain.model.Devolucion;
import com.policalum.erp.infrastructure.repository.projection.DevolucionesPorEstadoProjection;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Fechas siempre no nulas (FECHA_MIN/FECHA_MAX), rango semiabierto [desde, hasta). */
@Repository
public interface DevolucionRepository extends JpaRepository<Devolucion, UUID> {

    @EntityGraph(attributePaths = {"venta", "sede", "cliente", "solicitadoPor", "aprobadoPor"})
    @Query("SELECT d FROM Devolucion d WHERE d.id = :id")
    Optional<Devolucion> findDetalleById(@Param("id") UUID id);

    /** FOR UPDATE: evita aprobar/rechazar dos veces en paralelo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Devolucion d WHERE d.id = :id")
    Optional<Devolucion> findByIdParaActualizar(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"venta", "sede", "cliente"})
    @Query("SELECT d FROM Devolucion d WHERE " +
            "(CAST(:sedeId AS string) IS NULL OR d.sede.id = :sedeId) AND " +
            "(CAST(:estado AS string) IS NULL OR d.estado = :estado) AND " +
            "(CAST(:ventaId AS string) IS NULL OR d.venta.id = :ventaId) AND " +
            "d.fechaSolicitud >= CAST(:desde AS timestamp) AND d.fechaSolicitud < CAST(:hasta AS timestamp)")
    Page<Devolucion> search(@Param("sedeId") UUID sedeId,
                            @Param("estado") EstadoDevolucion estado,
                            @Param("ventaId") UUID ventaId,
                            @Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta,
                            Pageable pageable);

    @EntityGraph(attributePaths = {"venta", "sede", "cliente"})
    List<Devolucion> findByVenta_IdOrderByFechaSolicitudDesc(UUID ventaId);

    /** Usado por VentaServiceImpl.anular para impedir la doble reversión de stock. */
    boolean existsByVenta_IdAndEstadoIn(UUID ventaId, Collection<EstadoDevolucion> estados);

    @Query("SELECT d.estado AS estado, COUNT(d) AS cantidad, COALESCE(SUM(d.montoTotal), 0) AS monto " +
            "FROM Devolucion d WHERE (CAST(:sedeId AS string) IS NULL OR d.sede.id = :sedeId) AND " +
            "d.fechaSolicitud >= CAST(:desde AS timestamp) AND d.fechaSolicitud < CAST(:hasta AS timestamp) GROUP BY d.estado")
    List<DevolucionesPorEstadoProjection> totalesPorEstado(@Param("sedeId") UUID sedeId,
                                                    @Param("desde") LocalDateTime desde,
                                                    @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(SUM(d.montoTotal), 0) FROM Devolucion d WHERE d.estado = :estado AND " +
            "(CAST(:sedeId AS string) IS NULL OR d.sede.id = :sedeId) AND d.fechaResolucion >= CAST(:desde AS timestamp) AND d.fechaResolucion < CAST(:hasta AS timestamp)")
    BigDecimal sumarMontoResuelto(@Param("estado") EstadoDevolucion estado,
                                  @Param("sedeId") UUID sedeId,
                                  @Param("desde") LocalDateTime desde,
                                  @Param("hasta") LocalDateTime hasta);

    // ============ Módulo 6: utilidad neta ============

    /**
     * Devoluciones APROBADAS resueltas en [desde, hasta). Si el reembolso ya se procesó se usa
     * el monto realmente devuelto (puede ser menor por ajuste); si no, el monto de la devolución.
     * Montos CON IGV.
     */
    @Query("SELECT COALESCE(SUM(COALESCE(r.montoTotalReembolso, d.montoTotal)), 0) AS total, COUNT(d) AS cantidad " +
            "FROM Devolucion d LEFT JOIN Reembolso r ON r.devolucion = d AND " +
            "r.estado = com.policalum.erp.domain.enums.EstadoReembolso.PROCESADO " +
            "WHERE d.estado = com.policalum.erp.domain.enums.EstadoDevolucion.APROBADA AND " +
            "d.fechaResolucion >= CAST(:desde AS timestamp) AND d.fechaResolucion < CAST(:hasta AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR d.sede.id = :sedeId)")
    MontoCantidadProjection totalDevueltoPorPeriodo(@Param("sedeId") UUID sedeId,
                                                   @Param("desde") LocalDateTime desde,
                                                   @Param("hasta") LocalDateTime hasta);

    // ============ Módulo 12: reportes ============

    /**
     * Devoluciones APROBADAS resueltas en [desde, hasta) por cliente de la venta original; mismo criterio
     * de monto que totalDevueltoPorPeriodo (reembolso procesado si existe). id null = sin cliente.
     */
    @Query("SELECT v.cliente.id AS id, COALESCE(SUM(COALESCE(r.montoTotalReembolso, d.montoTotal)), 0) AS total, " +
            "COUNT(d) AS cantidad " +
            "FROM Devolucion d JOIN d.venta v LEFT JOIN Reembolso r ON r.devolucion = d AND " +
            "r.estado = com.policalum.erp.domain.enums.EstadoReembolso.PROCESADO " +
            "WHERE d.estado = com.policalum.erp.domain.enums.EstadoDevolucion.APROBADA AND " +
            "d.fechaResolucion >= CAST(:desde AS timestamp) AND d.fechaResolucion < CAST(:hasta AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR d.sede.id = :sedeId) " +
            "GROUP BY v.cliente.id")
    List<MontoCantidadPorIdProjection> devueltoPorCliente(@Param("desde") LocalDateTime desde,
                                                          @Param("hasta") LocalDateTime hasta,
                                                          @Param("sedeId") UUID sedeId);
}
