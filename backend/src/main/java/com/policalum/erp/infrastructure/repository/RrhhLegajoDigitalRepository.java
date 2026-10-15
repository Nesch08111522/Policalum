package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.enums.SistemaPension;
import com.policalum.erp.domain.model.RrhhLegajoDigital;
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

@Repository
public interface RrhhLegajoDigitalRepository extends JpaRepository<RrhhLegajoDigital, UUID> {

    Optional<RrhhLegajoDigital> findByUser_Id(UUID userId);

    boolean existsByUser_Id(UUID userId);

    /** Legajos activos de usuarios activos; sedeId null = todas las sedes. (Módulo 9) */
    @Query("SELECT l FROM RrhhLegajoDigital l JOIN FETCH l.user u JOIN FETCH u.sede s " +
            "WHERE l.activo = true AND u.activo = true AND (:sedeId IS NULL OR s.id = :sedeId) " +
            "ORDER BY u.apellido, u.nombre")
    List<RrhhLegajoDigital> findActivos(@Param("sedeId") UUID sedeId);

    // ============ Módulo 7 ============

    @EntityGraph(attributePaths = {"user", "user.sede"})
    @Query("SELECT l FROM RrhhLegajoDigital l WHERE l.user.id = :userId")
    Optional<RrhhLegajoDigital> findDetalleByUserId(@Param("userId") UUID userId);

    /**
     * Búsqueda paginada. patron siempre viene no nulo ("%" = sin filtro de texto)
     * para evitar parámetros String nulos dentro de lower()/like en PostgreSQL.
     */
    @EntityGraph(attributePaths = {"user", "user.sede"})
    @Query("SELECT l FROM RrhhLegajoDigital l WHERE " +
            "(:sedeId IS NULL OR l.user.sede.id = :sedeId) AND " +
            "(:tipoContrato IS NULL OR l.tipoContrato = :tipoContrato) AND " +
            "(:sistema IS NULL OR l.sistemaPension = :sistema) AND " +
            "(:activo IS NULL OR l.activo = :activo) AND " +
            "(LOWER(CONCAT(l.user.nombre, ' ', l.user.apellido)) LIKE :patron OR LOWER(l.user.email) LIKE :patron)")
    Page<RrhhLegajoDigital> search(@Param("sedeId") UUID sedeId,
                                   @Param("tipoContrato") String tipoContrato,
                                   @Param("sistema") SistemaPension sistema,
                                   @Param("activo") Boolean activo,
                                   @Param("patron") String patron,
                                   Pageable pageable);

    /** Todos los legajos (activos e inactivos) de la sede, para el dashboard. */
    @Query("SELECT l FROM RrhhLegajoDigital l JOIN FETCH l.user u WHERE (:sedeId IS NULL OR u.sede.id = :sedeId)")
    List<RrhhLegajoDigital> findTodos(@Param("sedeId") UUID sedeId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.activo = true AND (:sedeId IS NULL OR u.sede.id = :sedeId) " +
            "AND NOT EXISTS (SELECT 1 FROM RrhhLegajoDigital l WHERE l.user = u)")
    long contarUsuariosActivosSinLegajo(@Param("sedeId") UUID sedeId);
}
