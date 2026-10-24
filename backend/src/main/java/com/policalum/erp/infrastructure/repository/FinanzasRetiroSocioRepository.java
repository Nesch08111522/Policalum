package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.infrastructure.repository.projection.RetiroPorSedeOrigenProjection;
import com.policalum.erp.domain.enums.EstadoRetiro;
import com.policalum.erp.domain.enums.OrigenFondos;
import com.policalum.erp.domain.model.FinanzasRetiroSocio;
import com.policalum.erp.infrastructure.repository.projection.RetiroPorAdminProjection;
import com.policalum.erp.infrastructure.repository.projection.RetiroPorOrigenProjection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Las fechas desde/hasta se reciben SIEMPRE no nulas (el servicio pone los
 * valores por defecto) para evitar el error de PostgreSQL
 * "could not determine data type of parameter" con timestamps nulos.
 * Rango semiabierto: fechaRetiro >= desde AND fechaRetiro < hasta.
 */
@Repository
public interface FinanzasRetiroSocioRepository extends JpaRepository<FinanzasRetiroSocio, UUID> {

    @EntityGraph(attributePaths = {"admin", "sede", "turnoCaja", "anuladoPor"})
    @Query("SELECT r FROM FinanzasRetiroSocio r WHERE r.id = :id")
    Optional<FinanzasRetiroSocio> findDetalleById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"admin", "sede"})
    @Query("SELECT r FROM FinanzasRetiroSocio r WHERE " +
            "(CAST(:adminId AS string) IS NULL OR r.admin.id = :adminId) AND " +
            "(CAST(:sedeId AS string) IS NULL OR r.sede.id = :sedeId) AND " +
            "(CAST(:estado AS string) IS NULL OR r.estado = :estado) AND " +
            "(CAST(:origen AS string) IS NULL OR r.origenFondos = :origen) AND " +
            "r.fechaRetiro >= CAST(:desde AS timestamp) AND r.fechaRetiro < CAST(:hasta AS timestamp)")
    Page<FinanzasRetiroSocio> search(
            @Param("adminId") UUID adminId,
            @Param("sedeId") UUID sedeId,
            @Param("estado") EstadoRetiro estado,
            @Param("origen") OrigenFondos origen,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    @Query("SELECT r.origenFondos AS origen, COUNT(r) AS cantidad, " +
            "COALESCE(SUM(r.montoRetirado), 0) AS total " +
            "FROM FinanzasRetiroSocio r WHERE r.estado = :estado AND " +
            "(CAST(:sedeId AS string) IS NULL OR r.sede.id = :sedeId) AND " +
            "r.fechaRetiro >= CAST(:desde AS timestamp) AND r.fechaRetiro < CAST(:hasta AS timestamp) " +
            "GROUP BY r.origenFondos")
    List<RetiroPorOrigenProjection> totalesPorOrigen(
            @Param("estado") EstadoRetiro estado,
            @Param("sedeId") UUID sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT r.admin.id AS adminId, r.admin.nombre AS nombre, r.admin.apellido AS apellido, " +
            "COUNT(r) AS cantidad, COALESCE(SUM(r.montoRetirado), 0) AS total " +
            "FROM FinanzasRetiroSocio r WHERE r.estado = :estado AND " +
            "(CAST(:sedeId AS string) IS NULL OR r.sede.id = :sedeId) AND " +
            "r.fechaRetiro >= CAST(:desde AS timestamp) AND r.fechaRetiro < CAST(:hasta AS timestamp) " +
            "GROUP BY r.admin.id, r.admin.nombre, r.admin.apellido " +
            "ORDER BY SUM(r.montoRetirado) DESC")
    List<RetiroPorAdminProjection> totalesPorAdmin(
            @Param("estado") EstadoRetiro estado,
            @Param("sedeId") UUID sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(r) FROM FinanzasRetiroSocio r WHERE r.estado = :estado AND " +
            "(CAST(:sedeId AS string) IS NULL OR r.sede.id = :sedeId) AND " +
            "r.fechaRetiro >= CAST(:desde AS timestamp) AND r.fechaRetiro < CAST(:hasta AS timestamp)")
    long contarPorEstado(
            @Param("estado") EstadoRetiro estado,
            @Param("sedeId") UUID sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(SUM(r.montoRetirado), 0) FROM FinanzasRetiroSocio r WHERE r.estado = :estado AND " +
            "(CAST(:sedeId AS string) IS NULL OR r.sede.id = :sedeId) AND " +
            "r.fechaRetiro >= CAST(:desde AS timestamp) AND r.fechaRetiro < CAST(:hasta AS timestamp)")
    BigDecimal sumarMonto(
            @Param("estado") EstadoRetiro estado,
            @Param("sedeId") UUID sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // ============ Módulo 6: flujo de caja ============

    @Query("SELECT r.sede.id AS sedeId, r.origenFondos AS origen, COALESCE(SUM(r.montoRetirado), 0) AS total " +
            "FROM FinanzasRetiroSocio r WHERE r.estado = :estado AND r.fechaRetiro >= CAST(:desde AS timestamp) AND r.fechaRetiro < CAST(:hasta AS timestamp) AND " +
            "(CAST(:sedeId AS string) IS NULL OR r.sede.id = :sedeId) GROUP BY r.sede.id, r.origenFondos")
    List<RetiroPorSedeOrigenProjection> totalesPorSedeYOrigen(@Param("estado") EstadoRetiro estado,
                                                             @Param("desde") LocalDateTime desde,
                                                             @Param("hasta") LocalDateTime hasta,
                                                             @Param("sedeId") UUID sedeId);
}
