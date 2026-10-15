package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.enums.TipoDocumentoLegajo;
import com.policalum.erp.domain.model.RrhhLegajoDocumento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RrhhLegajoDocumentoRepository extends JpaRepository<RrhhLegajoDocumento, UUID> {

    @EntityGraph(attributePaths = {"subidoPor", "legajo"})
    List<RrhhLegajoDocumento> findByLegajo_IdOrderByCreatedAtDesc(UUID legajoId);

    /** Documento solo si pertenece al legajo indicado (evita acceder a docs de otro legajo). */
    @EntityGraph(attributePaths = {"legajo", "legajo.user"})
    Optional<RrhhLegajoDocumento> findByIdAndLegajo_Id(UUID id, UUID legajoId);

    @Query("SELECT DISTINCT d.legajo.id FROM RrhhLegajoDocumento d WHERE d.tipo = :tipo")
    List<UUID> findLegajoIdsConTipo(@Param("tipo") TipoDocumentoLegajo tipo);

    @Query("SELECT COUNT(d) FROM RrhhLegajoDocumento d WHERE (:sedeId IS NULL OR d.legajo.user.sede.id = :sedeId)")
    long contarPorSede(@Param("sedeId") UUID sedeId);
}
