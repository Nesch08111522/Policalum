package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.enums.EstadoComision;
import com.policalum.erp.domain.model.RrhhComision;
import jakarta.persistence.LockModeType;
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
public interface RrhhComisionRepository extends JpaRepository<RrhhComision, UUID> {

    Optional<RrhhComision> findByUser_IdAndPeriodo(UUID userId, String periodo);

    @EntityGraph(attributePaths = {"user", "sede", "aprobadoPor"})
    @Query("SELECT c FROM RrhhComision c WHERE c.id = :id")
    Optional<RrhhComision> findDetalleById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM RrhhComision c WHERE c.id = :id")
    Optional<RrhhComision> findByIdParaActualizar(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"user", "sede", "aprobadoPor"})
    @Query("SELECT c FROM RrhhComision c WHERE " +
            "(CAST(:periodo AS string) IS NULL OR c.periodo = :periodo) AND " +
            "(CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId) AND " +
            "(CAST(:estado AS string) IS NULL OR c.estado = :estado) AND " +
            "(CAST(:userId AS string) IS NULL OR c.user.id = :userId)")
    Page<RrhhComision> search(@Param("periodo") String periodo,
                              @Param("sedeId") UUID sedeId,
                              @Param("estado") EstadoComision estado,
                              @Param("userId") UUID userId,
                              Pageable pageable);

    @EntityGraph(attributePaths = {"user", "sede", "aprobadoPor"})
    @Query("SELECT c FROM RrhhComision c WHERE c.periodo = :periodo AND " +
            "(CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId) ORDER BY c.montoComision DESC")
    List<RrhhComision> findByPeriodo(@Param("periodo") String periodo, @Param("sedeId") UUID sedeId);

    long countByPeriodoAndEstadoAndSede_Id(String periodo, EstadoComision estado, UUID sedeId);
}
