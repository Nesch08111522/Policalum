package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.infrastructure.repository.projection.ReembolsoPorSedeMetodoProjection;
import com.policalum.erp.infrastructure.repository.projection.MontoPorSedeProjection;
import com.policalum.erp.domain.enums.EstadoReembolso;
import com.policalum.erp.domain.enums.MetodoReembolso;
import com.policalum.erp.domain.model.Reembolso;
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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReembolsoRepository extends JpaRepository<Reembolso, UUID> {

    @EntityGraph(attributePaths = {"devolucion", "venta", "cliente", "creadoPor", "procesadoPor", "turnoCaja"})
    @Query("SELECT r FROM Reembolso r WHERE r.id = :id")
    Optional<Reembolso> findDetalleById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reembolso r WHERE r.id = :id")
    Optional<Reembolso> findByIdParaActualizar(@Param("id") UUID id);

    Optional<Reembolso> findFirstByDevolucion_Id(UUID devolucionId);

    boolean existsByDevolucion_Id(UUID devolucionId);

    @EntityGraph(attributePaths = {"devolucion", "venta", "cliente", "creadoPor", "procesadoPor"})
    @Query("SELECT r FROM Reembolso r WHERE " +
            "(:sedeId IS NULL OR r.devolucion.sede.id = :sedeId) AND " +
            "(:estado IS NULL OR r.estado = :estado) AND " +
            "(:metodo IS NULL OR r.metodoReembolso = :metodo) AND " +
            "r.createdAt >= :desde AND r.createdAt < :hasta")
    Page<Reembolso> search(@Param("sedeId") UUID sedeId,
                           @Param("estado") EstadoReembolso estado,
                           @Param("metodo") MetodoReembolso metodo,
                           @Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta,
                           Pageable pageable);

    @Query("SELECT COUNT(r) FROM Reembolso r WHERE r.estado = com.policalum.erp.domain.enums.EstadoReembolso.PENDIENTE " +
            "AND (:sedeId IS NULL OR r.devolucion.sede.id = :sedeId)")
    long contarPendientes(@Param("sedeId") UUID sedeId);

    @Query("SELECT COALESCE(SUM(r.montoTotalReembolso), 0) FROM Reembolso r WHERE " +
            "r.estado = com.policalum.erp.domain.enums.EstadoReembolso.PENDIENTE AND " +
            "(:sedeId IS NULL OR r.devolucion.sede.id = :sedeId)")
    BigDecimal sumarPendientes(@Param("sedeId") UUID sedeId);

    @Query("SELECT COALESCE(SUM(r.montoTotalReembolso), 0) FROM Reembolso r WHERE " +
            "r.estado = com.policalum.erp.domain.enums.EstadoReembolso.PROCESADO AND " +
            "(:sedeId IS NULL OR r.devolucion.sede.id = :sedeId) AND " +
            "r.fechaProcesado >= :desde AND r.fechaProcesado < :hasta")
    BigDecimal sumarProcesados(@Param("sedeId") UUID sedeId,
                               @Param("desde") LocalDateTime desde,
                               @Param("hasta") LocalDateTime hasta);

    // ============ Módulo 6: flujo de caja y patrimonio ============

    @Query("SELECT r.devolucion.sede.id AS sedeId, r.metodoReembolso AS metodo, COALESCE(SUM(r.montoTotalReembolso), 0) AS total " +
            "FROM Reembolso r WHERE r.estado = com.policalum.erp.domain.enums.EstadoReembolso.PROCESADO AND " +
            "r.fechaProcesado >= :desde AND r.fechaProcesado < :hasta AND " +
            "(:sedeId IS NULL OR r.devolucion.sede.id = :sedeId) GROUP BY r.devolucion.sede.id, r.metodoReembolso")
    List<ReembolsoPorSedeMetodoProjection> procesadosPorSedeYMetodo(@Param("desde") LocalDateTime desde,
                                                                   @Param("hasta") LocalDateTime hasta,
                                                                   @Param("sedeId") UUID sedeId);

    @Query("SELECT r.devolucion.sede.id AS sedeId, COALESCE(SUM(r.montoTotalReembolso), 0) AS total FROM Reembolso r " +
            "WHERE r.estado = com.policalum.erp.domain.enums.EstadoReembolso.PENDIENTE AND " +
            "(:sedeId IS NULL OR r.devolucion.sede.id = :sedeId) GROUP BY r.devolucion.sede.id")
    List<MontoPorSedeProjection> pendientesPorSede(@Param("sedeId") UUID sedeId);
}
