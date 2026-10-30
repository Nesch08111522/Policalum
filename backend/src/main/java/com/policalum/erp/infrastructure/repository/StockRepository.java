package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.infrastructure.repository.projection.StockConsolidadoProjection;
import com.policalum.erp.infrastructure.repository.projection.ValorizacionProductoProjection;
import com.policalum.erp.infrastructure.repository.projection.SaldoProductoAlmacenProjection;
import com.policalum.erp.infrastructure.repository.projection.MontoPorSedeProjection;
import com.policalum.erp.domain.model.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID> {

    @Query("SELECT s FROM Stock s JOIN FETCH s.producto JOIN FETCH s.almacen a JOIN FETCH a.sede " +
            "WHERE s.stockMinimo > 0 AND s.cantidad <= s.stockMinimo")
    java.util.List<Stock> alertasDeStock();

    @EntityGraph(attributePaths = {"producto", "almacen"})
    Optional<Stock> findByAlmacen_IdAndProducto_Id(UUID almacenId, UUID productoId);

    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<Stock> findByAlmacen_Id(UUID almacenId);

    @EntityGraph(attributePaths = {"producto", "almacen"})
    List<Stock> findByProducto_Id(UUID productoId);

    /**
     * Búsqueda paginada de stock por almacén.
     *
     * IMPORTANTE:
     * - NO se usa TRIM(:searchTerm) dentro del JPQL (evita el bug de btrim(bytea) en PostgreSQL).
     * - La normalización de "" a null se hace en el servicio.
     * - codigoSku envuelto en COALESCE para que un producto sin SKU no rompa el LIKE.
     */
    @EntityGraph(attributePaths = {"producto", "almacen"})
    @Query("SELECT s FROM Stock s WHERE s.almacen.id = :almacenId AND " +
            "(CAST(:searchTerm AS string) IS NULL OR " +
            " LOWER(COALESCE(s.producto.nombre, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) OR " +
            " LOWER(COALESCE(s.producto.codigoSku, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))) AND " +
            "(:soloBajoStock = false OR s.cantidad <= s.stockMinimo)")
    Page<Stock> searchByAlmacen(
            @Param("almacenId") UUID almacenId,
            @Param("searchTerm") String searchTerm,
            @Param("soloBajoStock") boolean soloBajoStock,
            Pageable pageable
    );

    @Query("SELECT COUNT(s) FROM Stock s WHERE s.almacen.id = :almacenId AND s.cantidad <= s.stockMinimo")
    long countProductosBajoStockByAlmacen(@Param("almacenId") UUID almacenId);

    // ============ Módulo 6: patrimonio ============

    /** Inventario valorizado a precio de compra (solo cantidades positivas), por sede del almacén. */
    @Query("SELECT s.almacen.sede.id AS sedeId, COALESCE(SUM(s.cantidad * s.producto.precioCompra), 0) AS total " +
            "FROM Stock s WHERE s.cantidad > 0 AND (CAST(:sedeId AS string) IS NULL OR s.almacen.sede.id = :sedeId) " +
            "GROUP BY s.almacen.sede.id")
    List<MontoPorSedeProjection> valorizadoPorSede(@Param("sedeId") UUID sedeId);

    /** Filas de stock positivo cuyo producto no tiene costo (no suman al inventario). */
    @Query("SELECT COUNT(s) FROM Stock s WHERE s.cantidad > 0 AND " +
            "(s.producto.precioCompra IS NULL OR s.producto.precioCompra <= 0) AND " +
            "(CAST(:sedeId AS string) IS NULL OR s.almacen.sede.id = :sedeId)")
    long contarSinCosto(@Param("sedeId") UUID sedeId);

    // ============ Módulo 10: saldos y valorización ============

    /** FOR UPDATE: dos movimientos simultáneos del mismo producto/almacén no deben pisarse el saldo. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.almacen.id = :almacenId AND s.producto.id = :productoId")
    Optional<Stock> findParaActualizar(@Param("almacenId") UUID almacenId, @Param("productoId") UUID productoId);

    /**
     * Saldos con stock > 0 o con algún movimiento en el Kardex. busqueda ya viene en minúsculas
     * con comodines (%texto%) o null.
     *
     * codigoSku envuelto en COALESCE para que un producto sin SKU no rompa el LIKE.
     */
    String FILTROS_SALDOS = "(s.cantidad > 0 OR EXISTS (SELECT 1 FROM KardexMovimiento k WHERE k.producto = s.producto AND k.almacen = s.almacen)) AND " +
            "(CAST(:productoId AS string) IS NULL OR s.producto.id = :productoId) AND " +
            "(CAST(:almacenId AS string) IS NULL OR s.almacen.id = :almacenId) AND " +
            "(CAST(:sedeId AS string) IS NULL OR s.almacen.sede.id = :sedeId) AND " +
            "(CAST(:busqueda AS string) IS NULL OR LOWER(s.producto.nombre) LIKE CAST(:busqueda AS string) OR LOWER(COALESCE(s.producto.codigoSku, '')) LIKE CAST(:busqueda AS string)) AND " +
            "(:soloBajoMinimo = false OR (s.stockMinimo > 0 AND s.cantidad <= s.stockMinimo))";

    @Query(value = "SELECT s.producto.id AS productoId, s.producto.nombre AS productoNombre, s.producto.codigoSku AS codigoSku, " +
            "s.producto.unidad AS unidad, s.almacen.id AS almacenId, s.almacen.nombre AS almacenNombre, " +
            "s.almacen.sede.id AS sedeId, s.almacen.sede.nombre AS sedeNombre, s.cantidad AS cantidad, s.stockMinimo AS stockMinimo, " +
            "(SELECT MAX(k2.createdAt) FROM KardexMovimiento k2 WHERE k2.producto = s.producto AND k2.almacen = s.almacen) AS ultimoMovimiento " +
            "FROM Stock s WHERE " + FILTROS_SALDOS + " ORDER BY s.producto.nombre, s.almacen.nombre",
            countQuery = "SELECT COUNT(s) FROM Stock s WHERE " + FILTROS_SALDOS)
    Page<SaldoProductoAlmacenProjection> buscarSaldos(@Param("productoId") UUID productoId,
                                                      @Param("almacenId") UUID almacenId,
                                                      @Param("sedeId") UUID sedeId,
                                                      @Param("busqueda") String busqueda,
                                                      @Param("soloBajoMinimo") boolean soloBajoMinimo,
                                                      Pageable pageable);

    /** Existencias positivas por producto en los almacenes filtrados, con el costo promedio del producto. */
    @Query("SELECT p.id AS productoId, p.nombre AS productoNombre, p.codigoSku AS codigoSku, p.unidad AS unidad, " +
            "p.precioCompra AS costoUnitario, p.precioVentaBase AS precioVenta, SUM(s.cantidad) AS cantidad, COUNT(s) AS almacenes " +
            "FROM Stock s JOIN s.producto p WHERE s.cantidad > 0 AND " +
            "(CAST(:almacenId AS string) IS NULL OR s.almacen.id = :almacenId) AND (CAST(:sedeId AS string) IS NULL OR s.almacen.sede.id = :sedeId) " +
            "GROUP BY p.id, p.nombre, p.codigoSku, p.unidad, p.precioCompra, p.precioVentaBase")
    List<ValorizacionProductoProjection> valorizacion(@Param("almacenId") UUID almacenId, @Param("sedeId") UUID sedeId);

    // ============ Módulo 11: consolidado y dashboard ============

    /**
     * codigoSku envuelto en COALESCE para que un producto sin SKU no rompa el LIKE.
     */
    String FILTROS_CONSOLIDADO = "(s.cantidad > 0 OR s.stockMinimo > 0) AND " +
            "(CAST(:sedeId AS string) IS NULL OR s.almacen.sede.id = :sedeId) AND " +
            "(CAST(:busqueda AS string) IS NULL OR LOWER(p.nombre) LIKE CAST(:busqueda AS string) OR LOWER(COALESCE(p.codigoSku, '')) LIKE CAST(:busqueda AS string))";

    /** Stock por producto sumando los almacenes (los que tienen stock o un mínimo configurado). */
    @Query(value = "SELECT p.id AS productoId, p.nombre AS productoNombre, p.codigoSku AS codigoSku, p.unidad AS unidad, " +
            "COALESCE(SUM(s.cantidad), 0) AS stockTotal, COALESCE(SUM(s.stockMinimo), 0) AS stockMinimoTotal, COUNT(s) AS almacenes, " +
            "SUM(CASE WHEN s.stockMinimo > 0 AND s.cantidad <= s.stockMinimo THEN 1 ELSE 0 END) AS almacenesBajoMinimo " +
            "FROM Stock s JOIN s.producto p WHERE " + FILTROS_CONSOLIDADO +
            " GROUP BY p.id, p.nombre, p.codigoSku, p.unidad ORDER BY p.nombre",
            countQuery = "SELECT COUNT(DISTINCT p.id) FROM Stock s JOIN s.producto p WHERE " + FILTROS_CONSOLIDADO)
    Page<StockConsolidadoProjection> consolidado(@Param("sedeId") UUID sedeId,
                                                 @Param("busqueda") String busqueda,
                                                 Pageable pageable);

    @Query("SELECT COUNT(DISTINCT s.producto.id) FROM Stock s WHERE s.cantidad > 0 AND (CAST(:sedeId AS string) IS NULL OR s.almacen.sede.id = :sedeId)")
    long contarProductosConStock(@Param("sedeId") UUID sedeId);

    /** Filas con mínimo configurado y cantidad <= mínimo (incluye agotados). */
    @Query("SELECT COUNT(s) FROM Stock s WHERE s.stockMinimo > 0 AND s.cantidad <= s.stockMinimo AND " +
            "(CAST(:sedeId AS string) IS NULL OR s.almacen.sede.id = :sedeId)")
    long contarBajoMinimo(@Param("sedeId") UUID sedeId);

    @Query("SELECT COUNT(s) FROM Stock s WHERE s.stockMinimo > 0 AND s.cantidad <= 0 AND " +
            "(CAST(:sedeId AS string) IS NULL OR s.almacen.sede.id = :sedeId)")
    long contarAgotados(@Param("sedeId") UUID sedeId);

    /** Suma el saldo de los almacenes. La lista de ids no puede ir vacía. */
    @Query("SELECT s.producto.id, COALESCE(SUM(s.cantidad), 0) FROM Stock s WHERE s.producto.id IN :ids GROUP BY s.producto.id")
    List<Object[]> sumarCantidadPorProductos(@Param("ids") Collection<UUID> ids);
}
