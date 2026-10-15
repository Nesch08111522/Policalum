package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.CompraAdjunto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompraAdjuntoRepository extends JpaRepository<CompraAdjunto, UUID> {

    @EntityGraph(attributePaths = {"subidoPor"})
    List<CompraAdjunto> findByCompra_IdOrderByCreatedAtDesc(UUID compraId);

    long countByCompra_Id(UUID compraId);
}