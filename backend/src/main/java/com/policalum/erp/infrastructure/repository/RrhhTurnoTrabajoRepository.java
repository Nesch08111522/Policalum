package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.RrhhTurnoTrabajo;
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
public interface RrhhTurnoTrabajoRepository extends JpaRepository<RrhhTurnoTrabajo, UUID> {

    @EntityGraph(attributePaths = {"user", "sede"})
    @Query("SELECT t FROM RrhhTurnoTrabajo t WHERE t.id = :id")
    Optional<RrhhTurnoTrabajo> findDetalleById(@Param("id") UUID id);

    /** Turnos activos del empleado (para validar cruces, consultar y calcular tardanzas). */
    @EntityGraph(attributePaths = {"user", "sede"})
    @Query("SELECT t FROM RrhhTurnoTrabajo t WHERE t.user.id = :userId AND t.activo = true ORDER BY t.horaInicio")
    List<RrhhTurnoTrabajo> findActivosByUserId(@Param("userId") UUID userId);

    /** patron siempre no nulo ("%" = sin filtro) para evitar String nulos en lower()/like. */
    @EntityGraph(attributePaths = {"user", "sede"})
    @Query("SELECT t FROM RrhhTurnoTrabajo t WHERE " +
            "(:sedeId IS NULL OR t.sede.id = :sedeId) AND " +
            "(:userId IS NULL OR t.user.id = :userId) AND " +
            "(:activo IS NULL OR t.activo = :activo) AND " +
            "LOWER(t.nombreTurno) LIKE :patron")
    Page<RrhhTurnoTrabajo> search(@Param("sedeId") UUID sedeId,
                                  @Param("userId") UUID userId,
                                  @Param("activo") Boolean activo,
                                  @Param("patron") String patron,
                                  Pageable pageable);

    @Query("SELECT t FROM RrhhTurnoTrabajo t JOIN FETCH t.user u WHERE t.activo = true AND " +
            "(:sedeId IS NULL OR t.sede.id = :sedeId)")
    List<RrhhTurnoTrabajo> findActivosPorSede(@Param("sedeId") UUID sedeId);
}
