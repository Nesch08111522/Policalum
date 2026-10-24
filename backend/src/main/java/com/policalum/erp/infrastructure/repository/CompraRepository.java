package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.infrastructure.repository.projection.RecepcionCompraProjection;
import com.policalum.erp.infrastructure.repository.projection.ComprasPorProveedorProjection;
import com.policalum.erp.domain.enums.EstadoCompra;
import com.policalum.erp.domain.enums.Moneda;
import com.policalum.erp.infrastructure.repository.projection.TotalesFinancierosProjection;
import com.policalum.erp.domain.model.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompraRepository extends JpaRepository<Compra, UUID> {

    @EntityGraph(attributePaths = {"proveedor", "sede", "user"})
    @Query("SELECT c FROM Compra c WHERE " +
            "(CAST(:proveedorId AS string) IS NULL OR c.proveedor.id = :proveedorId) AND " +
            "(CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId) AND " +
            "(CAST(:estado AS string) IS NULL OR c.estadoCompra = :estado) AND " +
            "(CAST(:desde AS string) IS NULL OR c.fechaEmision >= CAST(CAST(:desde AS string) AS date)) AND " +
            "(CAST(:hasta AS string) IS NULL OR c.fechaEmision <= CAST(CAST(:hasta AS string) AS date))")
    Page<Compra> search(
            @Param("proveedorId") UUID proveedorId,
            @Param("sedeId") UUID sedeId,
            @Param("estado") EstadoCompra estado,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            Pageable pageable);

    @Query("SELECT c FROM Compra c " +
            "LEFT JOIN FETCH c.detalles d " +
            "LEFT JOIN FETCH d.producto " +
            "LEFT JOIN FETCH d.fraccion " +
            "LEFT JOIN FETCH d.almacen " +
            "LEFT JOIN FETCH c.adjuntos " +
            "LEFT JOIN FETCH c.proveedor " +
            "LEFT JOIN FETCH c.sede " +
            "LEFT JOIN FETCH c.user " +
            "WHERE c.id = :id")
    Optional<Compra> findWithDetailsById(@Param("id") UUID id);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(c.numeroOrdenCompra, 9, 4) AS int)), 0) " +
            "FROM Compra c WHERE c.numeroOrdenCompra LIKE CONCAT('OC-', :year, '-%')")
    Integer findMaxCorrelativoByYear(@Param("year") int year);

    long countByEstadoCompra(EstadoCompra estado);

    @EntityGraph(attributePaths = {"proveedor", "sede", "user"})
    Page<Compra> findByEstadoCompraOrderByCreatedAtDesc(EstadoCompra estado, Pageable pageable);

    // ============ Módulo 5: utilidad neta ============

    /** Totales de compras por fecha de emisión en [desde, hasta] (fechas inclusivas). */
    @Query("SELECT COALESCE(SUM(c.subtotalNeto), 0) AS neto, COALESCE(SUM(c.totalIgv), 0) AS igv, " +
            "COALESCE(SUM(c.total), 0) AS total, COUNT(c) AS cantidad FROM Compra c " +
            "WHERE c.estadoCompra = :estado AND c.moneda = :moneda AND " +
            "c.fechaEmision >= CAST(:desde AS date) AND c.fechaEmision <= CAST(:hasta AS date) AND " +
            "(CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId)")
    TotalesFinancierosProjection totalesPorPeriodo(@Param("estado") EstadoCompra estado,
                                                   @Param("moneda") Moneda moneda,
                                                   @Param("desde") LocalDate desde,
                                                   @Param("hasta") LocalDate hasta,
                                                   @Param("sedeId") UUID sedeId);

    /** Compras en otra moneda distinta a la indicada (se excluyen del cálculo y se informan). */
    @Query("SELECT COUNT(c) FROM Compra c WHERE c.estadoCompra = :estado AND c.moneda <> :moneda AND " +
            "c.fechaEmision >= CAST(:desde AS date) AND c.fechaEmision <= CAST(:hasta AS date) AND " +
            "(CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId)")
    long contarEnOtraMoneda(@Param("estado") EstadoCompra estado,
                            @Param("moneda") Moneda moneda,
                            @Param("desde") LocalDate desde,
                            @Param("hasta") LocalDate hasta,
                            @Param("sedeId") UUID sedeId);

    // ============ Módulo 12: reportes ============

    /** Compras por proveedor con fecha de emisión en [desde, hasta). Montos solo de RECIBIDAS en :moneda. */
    @Query("SELECT p.id AS proveedorId, p.nombreRazon AS nombre, p.numeroDocumento AS numeroDocumento, " +
            "COUNT(c) AS cantidad, " +
            "SUM(CASE WHEN c.estadoCompra = com.policalum.erp.domain.enums.EstadoCompra.RECIBIDA THEN 1 ELSE 0 END) AS recibidas, " +
            "SUM(CASE WHEN c.estadoCompra = com.policalum.erp.domain.enums.EstadoCompra.BORRADOR THEN 1 ELSE 0 END) AS pendientes, " +
            "SUM(CASE WHEN c.estadoCompra = com.policalum.erp.domain.enums.EstadoCompra.ANULADA THEN 1 ELSE 0 END) AS anuladas, " +
            "SUM(CASE WHEN c.estadoCompra = com.policalum.erp.domain.enums.EstadoCompra.RECIBIDA AND c.moneda = :moneda " +
            "THEN 1 ELSE 0 END) AS recibidasMonedaBase, " +
            "COALESCE(SUM(CASE WHEN c.estadoCompra = com.policalum.erp.domain.enums.EstadoCompra.RECIBIDA AND c.moneda = :moneda " +
            "THEN c.total ELSE 0 END), 0) AS monto, " +
            "MAX(c.fechaEmision) AS ultimaCompra " +
            "FROM Compra c JOIN c.proveedor p " +
            "WHERE c.fechaEmision >= CAST(:desde AS date) AND c.fechaEmision < CAST(:hasta AS date) AND (CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId) " +
            "GROUP BY p.id, p.nombreRazon, p.numeroDocumento")
    java.util.List<ComprasPorProveedorProjection> comprasPorProveedor(@Param("moneda") Moneda moneda,
                                                                      @Param("desde") LocalDate desde,
                                                                      @Param("hasta") LocalDate hasta,
                                                                      @Param("sedeId") UUID sedeId);

    /** Compras RECIBIDAS con emisión en [desde, hasta) y su recepción (primera ENTRADA de Kardex por la compra). */
    @Query("SELECT c.proveedor.id AS proveedorId, c.fechaEmision AS fechaEmision, MIN(k.createdAt) AS fechaRecepcion " +
            "FROM Compra c JOIN KardexMovimiento k ON k.referenciaId = c.id AND " +
            "k.tipoReferencia = com.policalum.erp.domain.enums.TipoReferencia.COMPRA AND " +
            "k.tipoMovimiento = com.policalum.erp.domain.enums.TipoMovimiento.ENTRADA " +
            "WHERE c.estadoCompra = com.policalum.erp.domain.enums.EstadoCompra.RECIBIDA AND " +
            "c.fechaEmision >= CAST(:desde AS date) AND c.fechaEmision < CAST(:hasta AS date) AND (CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId) " +
            "GROUP BY c.id, c.proveedor.id, c.fechaEmision")
    java.util.List<RecepcionCompraProjection> recepcionesCompra(@Param("desde") LocalDate desde,
                                                                @Param("hasta") LocalDate hasta,
                                                                @Param("sedeId") UUID sedeId);
}
