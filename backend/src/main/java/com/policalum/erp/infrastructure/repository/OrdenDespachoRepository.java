package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.enums.EstadoDespacho;
import com.policalum.erp.domain.model.OrdenDespacho;
import com.policalum.erp.infrastructure.repository.projection.DespachosPorEstadoProjection;
import com.policalum.erp.infrastructure.repository.projection.DespachosPorRepartidorProjection;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Fechas siempre no nulas (el servicio pone FECHA_MIN/FECHA_MAX). */
@Repository
public interface OrdenDespachoRepository extends JpaRepository<OrdenDespacho, UUID> {

    @EntityGraph(attributePaths = {"venta", "venta.cliente", "sede", "responsable", "movilidad"})
    @Query("SELECT o FROM OrdenDespacho o WHERE o.id = :id")
    Optional<OrdenDespacho> findDetalleById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"venta", "venta.cliente", "sede", "responsable", "movilidad"})
    @Query("SELECT o FROM OrdenDespacho o WHERE UPPER(o.codigoSeguimiento) = UPPER(:codigo)")
    Optional<OrdenDespacho> findByCodigo(@Param("codigo") String codigo);

    /** FOR UPDATE: serializa cambios de estado, asignación y entrega de la misma orden. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrdenDespacho o WHERE o.id = :id")
    Optional<OrdenDespacho> findByIdParaActualizar(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"venta", "venta.cliente", "sede", "responsable", "movilidad"})
    @Query("SELECT o FROM OrdenDespacho o WHERE " +
            "(:sedeId IS NULL OR o.sede.id = :sedeId) AND " +
            "(:estado IS NULL OR o.estado = :estado) AND " +
            "(:responsableId IS NULL OR o.responsable.id = :responsableId) AND " +
            "(:ventaId IS NULL OR o.venta.id = :ventaId) AND " +
            "((o.fechaProgramada >= :desde AND o.fechaProgramada <= :hasta) OR " +
            "(:incluirSinFecha = true AND o.fechaProgramada IS NULL))")
    Page<OrdenDespacho> search(@Param("sedeId") UUID sedeId,
                               @Param("estado") EstadoDespacho estado,
                               @Param("responsableId") UUID responsableId,
                               @Param("ventaId") UUID ventaId,
                               @Param("desde") LocalDate desde,
                               @Param("hasta") LocalDate hasta,
                               @Param("incluirSinFecha") boolean incluirSinFecha,
                               Pageable pageable);

    @EntityGraph(attributePaths = {"venta", "venta.cliente", "sede", "responsable", "movilidad"})
    @Query("SELECT o FROM OrdenDespacho o WHERE o.responsable.id = :userId AND o.estado IN :estados " +
            "ORDER BY o.fechaProgramada ASC, o.createdAt ASC")
    List<OrdenDespacho> findPorResponsable(@Param("userId") UUID userId,
                                           @Param("estados") Collection<EstadoDespacho> estados);

    // ============ Dashboard ============

    @Query("SELECT o.estado AS estado, COUNT(o) AS cantidad FROM OrdenDespacho o WHERE " +
            "(:sedeId IS NULL OR o.sede.id = :sedeId) AND o.fechaProgramada = :fecha GROUP BY o.estado")
    List<DespachosPorEstadoProjection> contarPorEstadoEnFecha(@Param("sedeId") UUID sedeId,
                                                              @Param("fecha") LocalDate fecha);

    /** Pendientes o en camino con fecha programada anterior a :fecha. */
    @Query("SELECT COUNT(o) FROM OrdenDespacho o WHERE (:sedeId IS NULL OR o.sede.id = :sedeId) AND " +
            "o.estado IN (com.policalum.erp.domain.enums.EstadoDespacho.PENDIENTE, " +
            "com.policalum.erp.domain.enums.EstadoDespacho.EN_CAMINO) AND o.fechaProgramada < :fecha")
    long contarAtrasados(@Param("sedeId") UUID sedeId, @Param("fecha") LocalDate fecha);

    @Query("SELECT COUNT(o) FROM OrdenDespacho o WHERE (:sedeId IS NULL OR o.sede.id = :sedeId) AND " +
            "o.estado = com.policalum.erp.domain.enums.EstadoDespacho.ENTREGADO AND " +
            "o.fechaEntrega >= :desde AND o.fechaEntrega < :hasta")
    long contarEntregados(@Param("sedeId") UUID sedeId, @Param("desde") LocalDateTime desde,
                          @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(o) FROM OrdenDespacho o WHERE (:sedeId IS NULL OR o.sede.id = :sedeId) AND " +
            "o.estado = com.policalum.erp.domain.enums.EstadoDespacho.ENTREGADO AND " +
            "o.fechaEntrega >= :desde AND o.fechaEntrega < :hasta AND COALESCE(o.intentosFallidos, 0) = 0")
    long contarEntregadosPrimerIntento(@Param("sedeId") UUID sedeId, @Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta);

    @Query("SELECT o.responsable.id AS responsableId, o.responsable.nombre AS nombre, o.responsable.apellido AS apellido, " +
            "SUM(CASE WHEN o.estado = com.policalum.erp.domain.enums.EstadoDespacho.PENDIENTE THEN 1 ELSE 0 END) AS pendientes, " +
            "SUM(CASE WHEN o.estado = com.policalum.erp.domain.enums.EstadoDespacho.EN_CAMINO THEN 1 ELSE 0 END) AS enCamino " +
            "FROM OrdenDespacho o WHERE o.responsable IS NOT NULL AND (:sedeId IS NULL OR o.sede.id = :sedeId) AND " +
            "o.estado IN (com.policalum.erp.domain.enums.EstadoDespacho.PENDIENTE, " +
            "com.policalum.erp.domain.enums.EstadoDespacho.EN_CAMINO) " +
            "GROUP BY o.responsable.id, o.responsable.nombre, o.responsable.apellido")
    List<DespachosPorRepartidorProjection> cargaPorRepartidor(@Param("sedeId") UUID sedeId);

    // ============ Módulo 16: movilidades ============

    /** Despachos de un vehículo en ciertos estados (desactivar exige que no haya PENDIENTE/EN_CAMINO). */
    long countByMovilidad_IdAndEstadoIn(UUID movilidadId, Collection<EstadoDespacho> estados);

    /** Vehículos distintos con algún despacho EN_CAMINO ahora. */
    @Query("SELECT COUNT(DISTINCT o.movilidad.id) FROM OrdenDespacho o WHERE o.movilidad IS NOT NULL AND " +
            "o.estado = com.policalum.erp.domain.enums.EstadoDespacho.EN_CAMINO AND (:sedeId IS NULL OR o.sede.id = :sedeId)")
    long contarVehiculosEnRuta(@Param("sedeId") UUID sedeId);

    @Query("SELECT COUNT(o) FROM OrdenDespacho o WHERE o.movilidad IS NULL AND (:sedeId IS NULL OR o.sede.id = :sedeId) AND " +
            "o.fechaProgramada >= :desde AND o.fechaProgramada <= :hasta")
    long contarSinMovilidad(@Param("sedeId") UUID sedeId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT COUNT(o) FROM OrdenDespacho o WHERE o.movilidad IS NOT NULL AND (:sedeId IS NULL OR o.sede.id = :sedeId) AND " +
            "o.estado = com.policalum.erp.domain.enums.EstadoDespacho.ENTREGADO AND o.fechaEntrega >= :desde AND o.fechaEntrega < :hasta")
    long contarEntregasConMovilidad(@Param("sedeId") UUID sedeId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Historial de un vehículo, más recientes primero. */
    @EntityGraph(attributePaths = {"venta", "venta.cliente", "sede", "responsable", "movilidad"})
    @Query(value = "SELECT o FROM OrdenDespacho o WHERE o.movilidad.id = :movilidadId AND (:estado IS NULL OR o.estado = :estado) " +
            "ORDER BY o.createdAt DESC, o.id DESC",
            countQuery = "SELECT COUNT(o) FROM OrdenDespacho o WHERE o.movilidad.id = :movilidadId AND (:estado IS NULL OR o.estado = :estado)")
    Page<OrdenDespacho> historialMovilidad(@Param("movilidadId") UUID movilidadId, @Param("estado") EstadoDespacho estado, Pageable pageable);
}
