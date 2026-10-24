package com.policalum.erp.infrastructure.repository;

import java.math.BigDecimal;
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

import com.policalum.erp.domain.model.CatalogoFraccion;

@Repository
public interface CatalogoFraccionRepository extends JpaRepository<CatalogoFraccion, UUID> {

    /**
     * Búsqueda paginada de fracciones.
     *
     * IMPORTANTE:
     * - Se elimina TRIM(:searchTerm) = '' para evitar btrim(bytea) en PostgreSQL.
     * - :searchTerm se castea a STRING dentro del JPQL para que PostgreSQL sepa que
     *   debe tratarlo como TEXT y no como BYTEA. Sin esto, cuando :searchTerm es null,
     *   PostgreSQL intenta aplicar lower()/like sobre un bytea y falla con:
     *       ERROR: no existe la función lower(bytea)
     *       ERROR: no existe la función pg_catalog.btrim(bytea)
     * - El servicio (CatalogoFraccionServiceImpl) normaliza "" a null antes de
     *   llamar a este método.
     */
    @EntityGraph(attributePaths = "producto")
    @Query("SELECT f FROM CatalogoFraccion f WHERE "
            + "(CAST(:productoId AS string) IS NULL OR f.producto.id = :productoId) AND "
            + "(CAST(:activo AS boolean) IS NULL OR f.activo = :activo) AND "
            + "(CAST(:searchTerm AS string) IS NULL OR "
            + " LOWER(f.nombreFraccion) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')))")
    Page<CatalogoFraccion> search(@Param("productoId") UUID productoId,
                                  @Param("activo") Boolean activo,
                                  @Param("searchTerm") String searchTerm,
                                  Pageable pageable);

    @EntityGraph(attributePaths = "producto")
    List<CatalogoFraccion> findByProducto_IdAndActivoTrueOrderByFactorDescuentoKardexDesc(UUID productoId);

    @EntityGraph(attributePaths = "producto")
    Optional<CatalogoFraccion> findWithProductoById(UUID id);

    Optional<CatalogoFraccion> findByProducto_IdAndFactorDescuentoKardex(UUID productoId,
                                                                          BigDecimal factorDescuentoKardex);

    long countByProducto_Id(UUID productoId);

    long countByActivo(Boolean activo);
}