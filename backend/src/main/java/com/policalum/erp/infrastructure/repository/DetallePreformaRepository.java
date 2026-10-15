package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.DetallePreforma;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DetallePreformaRepository extends JpaRepository<DetallePreforma, UUID> {

    @EntityGraph(attributePaths = {"producto", "fraccion", "servicio"})
    List<DetallePreforma> findByPreforma_Id(UUID preformaId);

    @Modifying
    @Query("DELETE FROM DetallePreforma d WHERE d.preforma.id = :preformaId")
    void eliminarPorPreforma(@Param("preformaId") UUID preformaId);
}
