package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.enums.EstadoPreforma;
import com.policalum.erp.domain.model.PreformaVenta;
import com.policalum.erp.infrastructure.repository.projection.PreformasPorEstadoProjection;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PreformaVentaRepository extends JpaRepository<PreformaVenta, UUID> {

    @EntityGraph(attributePaths = {"sede", "user", "cliente", "venta", "confirmadoPor", "canceladoPor"})
    @Query("SELECT p FROM PreformaVenta p WHERE p.id = :id")
    Optional<PreformaVenta> findDetalleById(@Param("id") UUID id);

    /** FOR UPDATE: confirmar/cancelar/editar/expirar no pueden pisarse (ej. doble confirmación). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PreformaVenta p WHERE p.id = :id")
    Optional<PreformaVenta> findByIdParaActualizar(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"user", "cliente"})
    List<PreformaVenta> findByUser_IdAndEstadoOrderByFechaExpiracionAsc(UUID userId, EstadoPreforma estado);

    @EntityGraph(attributePaths = {"user", "cliente"})
    @Query("SELECT p FROM PreformaVenta p WHERE p.estado = :estado AND " +
            "(CAST(:sedeId AS string) IS NULL OR p.sede.id = :sedeId) AND (CAST(:userId AS string) IS NULL OR p.user.id = :userId)")
    Page<PreformaVenta> buscarPorEstado(@Param("estado") EstadoPreforma estado,
                                        @Param("sedeId") UUID sedeId,
                                        @Param("userId") UUID userId,
                                        Pageable pageable);

    /**
     * Marca EXPIRADA toda ABIERTA vencida. En PostgreSQL (READ COMMITTED) el UPDATE re-evalúa
     * el WHERE tras esperar un FOR UPDATE de confirmar, así nunca expira una recién confirmada.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PreformaVenta p SET p.estado = com.policalum.erp.domain.enums.EstadoPreforma.EXPIRADA, " +
            "p.updatedAt = :ahora WHERE p.estado = com.policalum.erp.domain.enums.EstadoPreforma.ABIERTA " +
            "AND p.fechaExpiracion < CAST(:ahora AS timestamp)")
    int expirarVencidas(@Param("ahora") LocalDateTime ahora);

    @Query("SELECT p.estado AS estado, COUNT(p) AS cantidad, COALESCE(SUM(p.totalEstimado), 0) AS monto " +
            "FROM PreformaVenta p WHERE (CAST(:sedeId AS string) IS NULL OR p.sede.id = :sedeId) AND " +
            "p.createdAt >= CAST(:desde AS timestamp) AND p.createdAt < CAST(:hasta AS timestamp) GROUP BY p.estado")
    List<PreformasPorEstadoProjection> totalesPorEstado(@Param("sedeId") UUID sedeId,
                                                        @Param("desde") LocalDateTime desde,
                                                        @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(p) FROM PreformaVenta p WHERE p.estado = com.policalum.erp.domain.enums.EstadoPreforma.ABIERTA " +
            "AND p.fechaExpiracion >= CAST(:ahora AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR p.sede.id = :sedeId)")
    long contarAbiertasVigentes(@Param("sedeId") UUID sedeId, @Param("ahora") LocalDateTime ahora);

    @Query("SELECT COALESCE(SUM(p.totalEstimado), 0) FROM PreformaVenta p WHERE " +
            "p.estado = com.policalum.erp.domain.enums.EstadoPreforma.ABIERTA AND p.fechaExpiracion >= CAST(:ahora AS timestamp) " +
            "AND (CAST(:sedeId AS string) IS NULL OR p.sede.id = :sedeId)")
    BigDecimal sumarAbiertasVigentes(@Param("sedeId") UUID sedeId, @Param("ahora") LocalDateTime ahora);
}
