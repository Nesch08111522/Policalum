package com.policalum.erp.infrastructure.repository;

import java.util.Collection;
import java.time.LocalDateTime;
import com.policalum.erp.infrastructure.repository.projection.MontoPorSedeProjection;
import com.policalum.erp.infrastructure.repository.projection.MontoPorSedeMetodoProjection;
import com.policalum.erp.domain.enums.EstadoNomina;
import com.policalum.erp.domain.model.RrhhNomina;
import com.policalum.erp.infrastructure.repository.projection.NominaTotalesPorEstadoProjection;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
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

/**
 * El periodo de nómina es texto "YYYY-MM", así que la comparación por rango
 * de strings funciona igual que por fecha. Para utilidad neta se filtra por la
 * sede del trabajador (n.user.sede), así cubre también nóminas anteriores al
 * Módulo 9 que no tienen sede_id.
 */
@Repository
public interface RrhhNominaRepository extends JpaRepository<RrhhNomina, UUID> {

    // ============ Módulo 5: utilidad neta ============

    @Query("SELECT COALESCE(SUM(n.totalNeto), 0) FROM RrhhNomina n WHERE n.estado = :estado AND " +
            "n.periodo >= :periodoDesde AND n.periodo <= :periodoHasta AND " +
            "(CAST(:sedeId AS string) IS NULL OR n.user.sede.id = :sedeId)")
    BigDecimal sumarTotalNeto(
            @Param("estado") EstadoNomina estado,
            @Param("periodoDesde") String periodoDesde,
            @Param("periodoHasta") String periodoHasta,
            @Param("sedeId") UUID sedeId);

    @Query("SELECT COUNT(n) FROM RrhhNomina n WHERE n.estado = :estado AND " +
            "n.periodo >= :periodoDesde AND n.periodo <= :periodoHasta AND " +
            "(CAST(:sedeId AS string) IS NULL OR n.user.sede.id = :sedeId)")
    long contar(
            @Param("estado") EstadoNomina estado,
            @Param("periodoDesde") String periodoDesde,
            @Param("periodoHasta") String periodoHasta,
            @Param("sedeId") UUID sedeId);

    // ============ Módulo 9: nómina ============

    boolean existsByUser_IdAndPeriodo(UUID userId, String periodo);

    @EntityGraph(attributePaths = {"user", "sede", "comision", "generadoPor", "aprobadoPor", "pagadoPor"})
    @Query("SELECT n FROM RrhhNomina n WHERE n.id = :id")
    Optional<RrhhNomina> findDetalleById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM RrhhNomina n WHERE n.id = :id")
    Optional<RrhhNomina> findByIdParaActualizar(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"user", "sede"})
    @Query("SELECT n FROM RrhhNomina n WHERE " +
            "(CAST(:periodo AS string) IS NULL OR n.periodo = :periodo) AND " +
            "(CAST(:sedeId AS string) IS NULL OR n.user.sede.id = :sedeId) AND " +
            "(CAST(:estado AS string) IS NULL OR n.estado = :estado) AND " +
            "(CAST(:userId AS string) IS NULL OR n.user.id = :userId)")
    Page<RrhhNomina> search(@Param("periodo") String periodo,
                            @Param("sedeId") UUID sedeId,
                            @Param("estado") EstadoNomina estado,
                            @Param("userId") UUID userId,
                            Pageable pageable);

    @Query("SELECT n.estado AS estado, COUNT(n) AS cantidad, " +
            "COALESCE(SUM(n.sueldoBaseCalculado), 0) AS sueldos, " +
            "COALESCE(SUM(n.totalComisiones), 0) AS comisiones, " +
            "COALESCE(SUM(n.descuentos), 0) AS descuentos, " +
            "COALESCE(SUM(n.totalNeto), 0) AS neto " +
            "FROM RrhhNomina n WHERE n.periodo = :periodo AND " +
            "(CAST(:sedeId AS string) IS NULL OR n.user.sede.id = :sedeId) GROUP BY n.estado")
    List<NominaTotalesPorEstadoProjection> totalesPorEstado(@Param("periodo") String periodo,
                                                            @Param("sedeId") UUID sedeId);

    // ============ Módulo 6: flujo de caja y patrimonio ============

    /** Nóminas PAGADAS por fecha de pago real en [desde, hasta), por sede y método. */
    @Query("SELECT n.user.sede.id AS sedeId, n.metodoPago AS metodo, COALESCE(SUM(n.totalNeto), 0) AS total " +
            "FROM RrhhNomina n WHERE n.estado = com.policalum.erp.domain.enums.EstadoNomina.PAGADA AND " +
            "n.fechaPago >= CAST(:desde AS timestamp) AND n.fechaPago < CAST(:hasta AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR n.user.sede.id = :sedeId) " +
            "GROUP BY n.user.sede.id, n.metodoPago")
    List<MontoPorSedeMetodoProjection> pagadasPorSedeYMetodo(@Param("desde") LocalDateTime desde,
                                                            @Param("hasta") LocalDateTime hasta,
                                                            @Param("sedeId") UUID sedeId);

    /** Nóminas aún no pagadas (PENDIENTE/APROBADA), por sede. */
    @Query("SELECT n.user.sede.id AS sedeId, COALESCE(SUM(n.totalNeto), 0) AS total FROM RrhhNomina n " +
            "WHERE n.estado IN :estados AND (CAST(:sedeId AS string) IS NULL OR n.user.sede.id = :sedeId) GROUP BY n.user.sede.id")
    List<MontoPorSedeProjection> porPagarPorSede(@Param("estados") Collection<EstadoNomina> estados,
                                                 @Param("sedeId") UUID sedeId);
}
