package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.enums.TipoMovimiento;
import com.policalum.erp.domain.model.AjusteStock;
import com.policalum.erp.infrastructure.repository.projection.MontoCantidadProjection;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Fechas siempre no nulas (FECHA_MIN/FECHA_MAX), rango semiabierto [desde, hasta). */
public interface AjusteStockRepository extends JpaRepository<AjusteStock, UUID> {

    String FILTROS = "(:productoId IS NULL OR a.producto.id = :productoId) AND " +
            "(:almacenId IS NULL OR a.almacen.id = :almacenId) AND " +
            "(:sedeId IS NULL OR a.almacen.sede.id = :sedeId) AND " +
            "(:userId IS NULL OR a.user.id = :userId) AND " +
            "(:tipo IS NULL OR a.tipoMovimiento = :tipo) AND " +
            "a.createdAt >= :desde AND a.createdAt < :hasta";

    @EntityGraph(attributePaths = {"producto", "almacen", "almacen.sede", "user", "kardexMovimiento"})
    @Query("SELECT a FROM AjusteStock a WHERE a.id = :id")
    Optional<AjusteStock> findDetalleById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"producto", "almacen", "almacen.sede", "user", "kardexMovimiento"})
    @Query(value = "SELECT a FROM AjusteStock a WHERE " + FILTROS + " ORDER BY a.createdAt DESC, a.id DESC",
            countQuery = "SELECT COUNT(a) FROM AjusteStock a WHERE " + FILTROS)
    Page<AjusteStock> buscar(@Param("productoId") UUID productoId,
                             @Param("almacenId") UUID almacenId,
                             @Param("sedeId") UUID sedeId,
                             @Param("userId") UUID userId,
                             @Param("tipo") TipoMovimiento tipo,
                             @Param("desde") LocalDateTime desde,
                             @Param("hasta") LocalDateTime hasta,
                             Pageable pageable);

    /** Cantidad de ajustes y valor total (sin signo) en el rango. */
    @Query("SELECT COALESCE(SUM(a.valor), 0) AS total, COUNT(a) AS cantidad FROM AjusteStock a " +
            "WHERE a.createdAt >= :desde AND a.createdAt < :hasta AND (:sedeId IS NULL OR a.almacen.sede.id = :sedeId)")
    MontoCantidadProjection resumen(@Param("sedeId") UUID sedeId,
                                    @Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta);
}
