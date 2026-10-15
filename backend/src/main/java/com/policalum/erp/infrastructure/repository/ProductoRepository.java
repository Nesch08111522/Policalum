package com.policalum.erp.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.policalum.erp.domain.enums.TipoProducto;
import com.policalum.erp.domain.model.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    /**
     * Búsqueda paginada de productos.
     *
     * IMPORTANTE:
     * - El parámetro :searchTerm se castea a STRING dentro del JPQL para que PostgreSQL
     *   sepa que debe tratarlo como TEXT y no como BYTEA. Sin esto, cuando :searchTerm
     *   es null, PostgreSQL intenta aplicar lower()/like sobre un bytea y falla con:
     *       ERROR: no existe la función lower(bytea)
     *       ERROR: no existe la función pg_catalog.btrim(bytea)
     * - Se usa CONCAT('%', ..., '%') en lugar del operador || para que Hibernate lo
     *   traduzca de forma segura.
     * - El servicio normaliza "" a null antes de llamar a este método.
     */
    @EntityGraph(attributePaths = { "categoria", "proveedor", "fracciones" })
    @Query("SELECT p FROM Producto p WHERE "
            + "(:searchTerm IS NULL OR "
            + " LOWER(p.nombre) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) OR "
            + " LOWER(COALESCE(p.codigoSku, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))) AND "
            + "(:categoriaId IS NULL OR p.categoria.id = :categoriaId) AND "
            + "(:tipo IS NULL OR p.tipo = :tipo) AND "
            + "(:activo IS NULL OR p.activo = :activo) AND "
            + "(:esFraccionable IS NULL OR p.esFraccionable = :esFraccionable)")
    Page<Producto> search(@Param("searchTerm") String searchTerm,
                          @Param("categoriaId") UUID categoriaId,
                          @Param("tipo") TipoProducto tipo,
                          @Param("activo") Boolean activo,
                          @Param("esFraccionable") Boolean esFraccionable,
                          Pageable pageable);

    @Query("SELECT p FROM Producto p "
            + "LEFT JOIN FETCH p.categoria "
            + "LEFT JOIN FETCH p.proveedor "
            + "LEFT JOIN FETCH p.fracciones "
            + "WHERE p.id = :id")
    Optional<Producto> findWithRelationsById(@Param("id") UUID id);

    Optional<Producto> findByCodigoSkuIgnoreCase(String codigoSku);

    boolean existsByCodigoSkuIgnoreCase(String codigoSku);

    boolean existsByIdAndEsFraccionableTrue(UUID id);

    long countByActivo(Boolean activo);

    long countByEsFraccionable(Boolean esFraccionable);

    long countByTipo(TipoProducto tipo);
}