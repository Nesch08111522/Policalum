package com.policalum.erp.infrastructure.repository;

import java.util.Optional;
import com.policalum.erp.infrastructure.repository.projection.TotalPorTipoMovimientoProjection;
import com.policalum.erp.infrastructure.repository.projection.MermaProjection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.policalum.erp.domain.model.KardexMovimiento;
import com.policalum.erp.domain.enums.TipoReferencia;

@Repository
public interface KardexRepository extends JpaRepository<KardexMovimiento, UUID> {

    @EntityGraph(attributePaths = {"producto", "almacen", "user"})
    @Query("SELECT k FROM KardexMovimiento k WHERE " +
            "(CAST(:productoId AS string) IS NULL OR k.producto.id = :productoId) AND " +
            "(CAST(:almacenId AS string) IS NULL OR k.almacen.id = :almacenId) AND " +
            "k.createdAt >= CAST(:fechaInicio AS timestamp) AND " +
            "k.createdAt <= CAST(:fechaFin AS timestamp)")
    Page<KardexMovimiento> searchMovimientos(
            @Param("productoId") UUID productoId,
            @Param("almacenId") UUID almacenId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"producto", "almacen", "user"})
    List<KardexMovimiento> findByReferenciaIdAndTipoReferencia(UUID referenciaId, TipoReferencia tipoReferencia);

    // ============ Módulo 6: mermas valorizadas ============

    /** Valor de las salidas por MERMA en [desde, hasta), a precio de compra del producto. */
    @Query("SELECT COALESCE(SUM(k.cantidadMovida * k.producto.precioCompra), 0) FROM KardexMovimiento k " +
            "WHERE k.tipoReferencia = com.policalum.erp.domain.enums.TipoReferencia.MERMA AND " +
            "k.tipoMovimiento = com.policalum.erp.domain.enums.TipoMovimiento.SALIDA AND " +
            "k.createdAt >= CAST(:desde AS timestamp) AND k.createdAt < CAST(:hasta AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR k.almacen.sede.id = :sedeId)")
    BigDecimal valorMermas(@Param("sedeId") UUID sedeId,
                           @Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta);

    // ============ Módulo 10: consultas avanzadas ============
    // Fechas siempre no nulas, rango semiabierto [desde, hasta). incluirSinFecha = true agrega
    // los movimientos antiguos guardados con created_at NULL (antes del fix del Módulo 10).

    String FILTROS_KARDEX = "(CAST(:productoId AS string) IS NULL OR k.producto.id = :productoId) AND " +
            "(CAST(:almacenId AS string) IS NULL OR k.almacen.id = :almacenId) AND " +
            "(CAST(:tipoReferencia AS string) IS NULL OR k.tipoReferencia = :tipoReferencia) AND " +
            "((k.createdAt >= CAST(:desde AS timestamp) AND k.createdAt < CAST(:hasta AS timestamp)) OR (:incluirSinFecha = true AND k.createdAt IS NULL))";

    @EntityGraph(attributePaths = {"producto", "almacen", "almacen.sede", "user"})
    @Query("SELECT k FROM KardexMovimiento k WHERE k.id = :id")
    Optional<KardexMovimiento> findDetalleById(@Param("id") UUID id);

    /** Más recientes primero; los movimientos sin fecha al final. */
    @EntityGraph(attributePaths = {"producto", "almacen", "almacen.sede", "user"})
    @Query(value = "SELECT k FROM KardexMovimiento k WHERE " + FILTROS_KARDEX + " ORDER BY k.createdAt DESC NULLS LAST, k.id DESC",
            countQuery = "SELECT COUNT(k) FROM KardexMovimiento k WHERE " + FILTROS_KARDEX)
    Page<KardexMovimiento> buscar(@Param("productoId") UUID productoId,
                                  @Param("almacenId") UUID almacenId,
                                  @Param("tipoReferencia") TipoReferencia tipoReferencia,
                                  @Param("desde") LocalDateTime desde,
                                  @Param("hasta") LocalDateTime hasta,
                                  @Param("incluirSinFecha") boolean incluirSinFecha,
                                  Pageable pageable);

    /** Orden cronológico estable (fecha, id) para exportar por lotes. */
    @EntityGraph(attributePaths = {"producto", "almacen", "almacen.sede", "user"})
    @Query(value = "SELECT k FROM KardexMovimiento k WHERE " + FILTROS_KARDEX + " ORDER BY k.createdAt ASC NULLS FIRST, k.id ASC",
            countQuery = "SELECT COUNT(k) FROM KardexMovimiento k WHERE " + FILTROS_KARDEX)
    Page<KardexMovimiento> buscarParaExportar(@Param("productoId") UUID productoId,
                                              @Param("almacenId") UUID almacenId,
                                              @Param("tipoReferencia") TipoReferencia tipoReferencia,
                                              @Param("desde") LocalDateTime desde,
                                              @Param("hasta") LocalDateTime hasta,
                                              @Param("incluirSinFecha") boolean incluirSinFecha,
                                              Pageable pageable);

    /** Mermas (tipoReferencia = MERMA, SALIDA) agrupadas por producto, valor = cantidad × costo promedio actual. */
    @Query("SELECT p.id AS productoId, p.nombre AS productoNombre, p.codigoSku AS codigoSku, p.unidad AS unidad, " +
            "COUNT(k) AS movimientos, COALESCE(SUM(k.cantidadMovida), 0) AS cantidad, " +
            "COALESCE(SUM(k.cantidadMovida * p.precioCompra), 0) AS valor " +
            "FROM KardexMovimiento k JOIN k.producto p " +
            "WHERE k.tipoReferencia = com.policalum.erp.domain.enums.TipoReferencia.MERMA AND " +
            "k.tipoMovimiento = com.policalum.erp.domain.enums.TipoMovimiento.SALIDA AND " +
            "(CAST(:productoId AS string) IS NULL OR p.id = :productoId) AND (CAST(:almacenId AS string) IS NULL OR k.almacen.id = :almacenId) AND " +
            "((k.createdAt >= CAST(:desde AS timestamp) AND k.createdAt < CAST(:hasta AS timestamp)) OR (:incluirSinFecha = true AND k.createdAt IS NULL)) " +
            "GROUP BY p.id, p.nombre, p.codigoSku, p.unidad ORDER BY COALESCE(SUM(k.cantidadMovida * p.precioCompra), 0) DESC")
    List<MermaProjection> mermasPorProducto(@Param("productoId") UUID productoId,
                                            @Param("almacenId") UUID almacenId,
                                            @Param("desde") LocalDateTime desde,
                                            @Param("hasta") LocalDateTime hasta,
                                            @Param("incluirSinFecha") boolean incluirSinFecha);

    /** Entradas y salidas de un producto en el rango (para el historial). */
    @Query("SELECT k.tipoMovimiento AS tipo, COALESCE(SUM(k.cantidadMovida), 0) AS cantidad, COUNT(k) AS movimientos " +
            "FROM KardexMovimiento k WHERE k.producto.id = :productoId AND (CAST(:almacenId AS string) IS NULL OR k.almacen.id = :almacenId) AND " +
            "((k.createdAt >= CAST(:desde AS timestamp) AND k.createdAt < CAST(:hasta AS timestamp)) OR (:incluirSinFecha = true AND k.createdAt IS NULL)) " +
            "GROUP BY k.tipoMovimiento")
    List<TotalPorTipoMovimientoProjection> totalesPorTipo(@Param("productoId") UUID productoId,
                                                          @Param("almacenId") UUID almacenId,
                                                          @Param("desde") LocalDateTime desde,
                                                          @Param("hasta") LocalDateTime hasta,
                                                          @Param("incluirSinFecha") boolean incluirSinFecha);

    /** Detalle de mermas: solo SALIDAS con tipoReferencia = MERMA, más recientes primero. */
    @EntityGraph(attributePaths = {"producto", "almacen", "user"})
    @Query(value = "SELECT k FROM KardexMovimiento k WHERE k.tipoReferencia = com.policalum.erp.domain.enums.TipoReferencia.MERMA AND " +
            "k.tipoMovimiento = com.policalum.erp.domain.enums.TipoMovimiento.SALIDA AND " +
            "(CAST(:productoId AS string) IS NULL OR k.producto.id = :productoId) AND (CAST(:almacenId AS string) IS NULL OR k.almacen.id = :almacenId) AND " +
            "((k.createdAt >= CAST(:desde AS timestamp) AND k.createdAt < CAST(:hasta AS timestamp)) OR (:incluirSinFecha = true AND k.createdAt IS NULL)) " +
            "ORDER BY k.createdAt DESC NULLS LAST, k.id DESC",
            countQuery = "SELECT COUNT(k) FROM KardexMovimiento k WHERE k.tipoReferencia = com.policalum.erp.domain.enums.TipoReferencia.MERMA AND " +
            "k.tipoMovimiento = com.policalum.erp.domain.enums.TipoMovimiento.SALIDA AND " +
            "(CAST(:productoId AS string) IS NULL OR k.producto.id = :productoId) AND (CAST(:almacenId AS string) IS NULL OR k.almacen.id = :almacenId) AND " +
            "((k.createdAt >= CAST(:desde AS timestamp) AND k.createdAt < CAST(:hasta AS timestamp)) OR (:incluirSinFecha = true AND k.createdAt IS NULL))")
    Page<KardexMovimiento> buscarMermas(@Param("productoId") UUID productoId,
                                        @Param("almacenId") UUID almacenId,
                                        @Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta,
                                        @Param("incluirSinFecha") boolean incluirSinFecha,
                                        Pageable pageable);

    /** Movimientos anteriores al fix del Módulo 10 (created_at NULL). */
    long countByCreatedAtIsNull();

    // ============ Módulo 11: dashboard de stock ============

    /** Movimientos por tipo en [desde, hasta) (para "movimientos del día"). */
    @Query("SELECT k.tipoMovimiento AS tipo, COALESCE(SUM(k.cantidadMovida), 0) AS cantidad, COUNT(k) AS movimientos " +
            "FROM KardexMovimiento k WHERE k.createdAt >= CAST(:desde AS timestamp) AND k.createdAt < CAST(:hasta AS timestamp) AND " +
            "(CAST(:sedeId AS string) IS NULL OR k.almacen.sede.id = :sedeId) GROUP BY k.tipoMovimiento")
    List<TotalPorTipoMovimientoProjection> conteoPorTipo(@Param("sedeId") UUID sedeId,
                                                         @Param("desde") LocalDateTime desde,
                                                         @Param("hasta") LocalDateTime hasta);
}
