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
            + "(CAST(:searchTerm AS string) IS NULL OR "
            + " LOWER(p.nombre) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) OR "
            + " LOWER(COALESCE(p.codigoSku, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))) AND "
            + "(CAST(:categoriaId AS string) IS NULL OR p.categoria.id = :categoriaId) AND "
            + "(CAST(:tipo AS string) IS NULL OR p.tipo = :tipo) AND "
            + "(CAST(:activo AS boolean) IS NULL OR p.activo = :activo) AND "
            + "(CAST(:esFraccionable AS boolean) IS NULL OR p.esFraccionable = :esFraccionable)")
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

    /**
     * Catálogo del portal: solo productos activos. q ya viene sin espacios o null.
     * soloDestacados en false no filtra el flag.
     */
    @Query(value = "SELECT p FROM Producto p WHERE p.activo = true AND "
            + "(CAST(:categoriaId AS string) IS NULL OR p.categoria.id = :categoriaId) AND "
            + "(CAST(:soloDestacados AS boolean) = false OR p.destacado = true) AND "
            + "(CAST(:q AS string) IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR "
            + " LOWER(COALESCE(p.codigoSku, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))",
            countQuery = "SELECT COUNT(p) FROM Producto p WHERE p.activo = true AND "
            + "(CAST(:categoriaId AS string) IS NULL OR p.categoria.id = :categoriaId) AND "
            + "(CAST(:soloDestacados AS boolean) = false OR p.destacado = true) AND "
            + "(CAST(:q AS string) IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR "
            + " LOWER(COALESCE(p.codigoSku, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))")
    Page<Producto> catalogoPublico(@Param("categoriaId") UUID categoriaId,
                                   @Param("soloDestacados") boolean soloDestacados,
                                   @Param("q") String q,
                                   Pageable pageable);

    Optional<Producto> findByCodigoSkuIgnoreCase(String codigoSku);

    boolean existsByCodigoSkuIgnoreCase(String codigoSku);

    boolean existsByIdAndEsFraccionableTrue(UUID id);

    long countByActivo(Boolean activo);

    long countByEsFraccionable(Boolean esFraccionable);

    long countByTipo(TipoProducto tipo);

    /**
     * El producto participa en stock, kardex, compras, ventas u otros documentos.
     * En ese caso no se borra: se desactiva para conservar el historial.
     */
    @Query("""
            SELECT CASE WHEN
                EXISTS (SELECT 1 FROM KardexMovimiento k WHERE k.producto.id = :id)
                OR EXISTS (SELECT 1 FROM Stock s WHERE s.producto.id = :id)
                OR EXISTS (SELECT 1 FROM AjusteStock a WHERE a.producto.id = :id)
                OR EXISTS (SELECT 1 FROM TransferenciaStock t WHERE t.producto.id = :id)
                OR EXISTS (SELECT 1 FROM DetalleVenta d WHERE d.producto.id = :id OR d.fraccion.producto.id = :id)
                OR EXISTS (SELECT 1 FROM DetalleCompra d WHERE d.producto.id = :id OR d.fraccion.producto.id = :id)
                OR EXISTS (SELECT 1 FROM DetalleCotizacion d WHERE d.producto.id = :id OR d.fraccion.producto.id = :id)
                OR EXISTS (SELECT 1 FROM DetallePreforma d WHERE d.producto.id = :id OR d.fraccion.producto.id = :id)
                OR EXISTS (SELECT 1 FROM DetalleDevolucion d WHERE d.producto.id = :id OR d.fraccion.producto.id = :id)
            THEN TRUE ELSE FALSE END
            """)
    boolean tieneHistorial(@Param("id") UUID id);
}