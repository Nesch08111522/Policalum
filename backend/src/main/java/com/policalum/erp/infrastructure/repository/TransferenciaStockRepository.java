package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.TransferenciaStock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * almacenId / sedeId filtran por origen O destino. Fechas siempre no nulas, rango
 * semiabierto [desde, hasta).
 */
public interface TransferenciaStockRepository extends JpaRepository<TransferenciaStock, UUID> {

    String FILTROS = "(CAST(:productoId AS string) IS NULL OR t.producto.id = :productoId) AND " +
            "(CAST(:almacenId AS string) IS NULL OR t.almacenOrigen.id = :almacenId OR t.almacenDestino.id = :almacenId) AND " +
            "(CAST(:sedeId AS string) IS NULL OR t.almacenOrigen.sede.id = :sedeId OR t.almacenDestino.sede.id = :sedeId) AND " +
            "(:soloEntreSedes = false OR t.entreSedes = true) AND " +
            "t.createdAt >= CAST(:desde AS timestamp) AND t.createdAt < CAST(:hasta AS timestamp)";

    @EntityGraph(attributePaths = {"producto", "almacenOrigen", "almacenOrigen.sede", "almacenDestino", "almacenDestino.sede",
            "user", "movimientoSalida", "movimientoEntrada"})
    @Query("SELECT t FROM TransferenciaStock t WHERE t.id = :id")
    Optional<TransferenciaStock> findDetalleById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"producto", "almacenOrigen", "almacenOrigen.sede", "almacenDestino", "almacenDestino.sede",
            "user", "movimientoSalida", "movimientoEntrada"})
    @Query(value = "SELECT t FROM TransferenciaStock t WHERE " + FILTROS + " ORDER BY t.createdAt DESC, t.id DESC",
            countQuery = "SELECT COUNT(t) FROM TransferenciaStock t WHERE " + FILTROS)
    Page<TransferenciaStock> buscar(@Param("productoId") UUID productoId,
                                    @Param("almacenId") UUID almacenId,
                                    @Param("sedeId") UUID sedeId,
                                    @Param("soloEntreSedes") boolean soloEntreSedes,
                                    @Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta,
                                    Pageable pageable);

    @Query("SELECT COUNT(t) FROM TransferenciaStock t WHERE t.createdAt >= CAST(:desde AS timestamp) AND t.createdAt < CAST(:hasta AS timestamp) AND " +
            "(CAST(:sedeId AS string) IS NULL OR t.almacenOrigen.sede.id = :sedeId OR t.almacenDestino.sede.id = :sedeId)")
    long contar(@Param("sedeId") UUID sedeId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
