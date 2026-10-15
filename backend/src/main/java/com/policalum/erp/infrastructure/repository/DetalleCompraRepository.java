package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.DetalleCompra;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, UUID> {

    @EntityGraph(attributePaths = {"producto", "fraccion", "almacen"})
    List<DetalleCompra> findByCompra_Id(UUID compraId);
}