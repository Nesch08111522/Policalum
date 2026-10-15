package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.Movilidad;
import com.policalum.erp.infrastructure.repository.projection.ConteoPorTextoProjection;
import com.policalum.erp.infrastructure.repository.projection.UsoMovilidadProjection;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
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

public interface MovilidadRepository extends JpaRepository<Movilidad, UUID> {

    /** Usado por OrdenDespachoServiceImpl (Módulo 4). */
    List<Movilidad> findByConductor_IdAndActivoTrue(UUID conductorId);

    // ============ Módulo 16 ============

    @EntityGraph(attributePaths = {"sede", "conductor"})
    @Query("SELECT m FROM Movilidad m WHERE m.id = :id")
    Optional<Movilidad> findDetalleById(@Param("id") UUID id);

    /** FOR UPDATE: desactivar y asignar conductor no deben pisarse. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Movilidad m WHERE m.id = :id")
    Optional<Movilidad> findByIdParaActualizar(@Param("id") UUID id);

    /** Placa única en la empresa, comparada en mayúsculas y sin guiones ni espacios. */
    @Query("SELECT COUNT(m) > 0 FROM Movilidad m WHERE " +
            "REPLACE(REPLACE(UPPER(m.placa), '-', ''), ' ', '') = :placaNormalizada AND m.id <> :excluirId")
    boolean existePlaca(@Param("placaNormalizada") String placaNormalizada, @Param("excluirId") UUID excluirId);

    /** texto ya viene en minúsculas con comodines (%texto%) o null. */
    @EntityGraph(attributePaths = {"sede", "conductor"})
    @Query(value = "SELECT m FROM Movilidad m LEFT JOIN m.conductor c WHERE " +
            "(:sedeId IS NULL OR m.sede.id = :sedeId) AND (:activo IS NULL OR m.activo = :activo) AND " +
            "(:tipo IS NULL OR m.tipo = :tipo) AND (:texto IS NULL OR LOWER(m.placa) LIKE :texto OR " +
            "LOWER(m.marca) LIKE :texto OR LOWER(m.modelo) LIKE :texto OR " +
            "LOWER(CONCAT(c.nombre, ' ', c.apellido)) LIKE :texto) ORDER BY m.placa",
            countQuery = "SELECT COUNT(m) FROM Movilidad m LEFT JOIN m.conductor c WHERE " +
            "(:sedeId IS NULL OR m.sede.id = :sedeId) AND (:activo IS NULL OR m.activo = :activo) AND " +
            "(:tipo IS NULL OR m.tipo = :tipo) AND (:texto IS NULL OR LOWER(m.placa) LIKE :texto OR " +
            "LOWER(m.marca) LIKE :texto OR LOWER(m.modelo) LIKE :texto OR " +
            "LOWER(CONCAT(c.nombre, ' ', c.apellido)) LIKE :texto)")
    Page<Movilidad> buscar(@Param("sedeId") UUID sedeId,
                           @Param("activo") Boolean activo,
                           @Param("tipo") String tipo,
                           @Param("texto") String texto,
                           Pageable pageable);

    /**
     * Despachos por vehículo con fecha programada en [desde, hasta] (fechas inclusivas).
     * Incluye los vehículos activos sin despachos y los inactivos que sí tuvieron.
     */
    @Query("SELECT m.id AS movilidadId, m.placa AS placa, m.tipo AS tipo, c.nombre AS conductorNombre, " +
            "c.apellido AS conductorApellido, m.activo AS activo, COUNT(o) AS despachos, " +
            "SUM(CASE WHEN o.estado = com.policalum.erp.domain.enums.EstadoDespacho.ENTREGADO THEN 1 ELSE 0 END) AS entregados, " +
            "SUM(CASE WHEN o.estado = com.policalum.erp.domain.enums.EstadoDespacho.FALLIDO THEN 1 ELSE 0 END) AS fallidos, " +
            "SUM(CASE WHEN o.estado IN (com.policalum.erp.domain.enums.EstadoDespacho.PENDIENTE, " +
            "com.policalum.erp.domain.enums.EstadoDespacho.EN_CAMINO) THEN 1 ELSE 0 END) AS pendientes, " +
            "COALESCE(SUM(o.intentosFallidos), 0) AS intentosFallidos " +
            "FROM Movilidad m LEFT JOIN m.conductor c " +
            "LEFT JOIN OrdenDespacho o ON o.movilidad = m AND o.fechaProgramada >= :desde AND o.fechaProgramada <= :hasta " +
            "WHERE (:sedeId IS NULL OR m.sede.id = :sedeId) " +
            "GROUP BY m.id, m.placa, m.tipo, c.nombre, c.apellido, m.activo " +
            "HAVING m.activo = true OR COUNT(o) > 0")
    List<UsoMovilidadProjection> uso(@Param("sedeId") UUID sedeId,
                                     @Param("desde") LocalDate desde,
                                     @Param("hasta") LocalDate hasta);

    @Query("SELECT COUNT(m) FROM Movilidad m WHERE (:sedeId IS NULL OR m.sede.id = :sedeId) AND m.activo = :activo")
    long contarPorEstado(@Param("sedeId") UUID sedeId, @Param("activo") boolean activo);

    @Query("SELECT COUNT(m) FROM Movilidad m WHERE (:sedeId IS NULL OR m.sede.id = :sedeId) AND m.activo = true AND m.conductor IS NULL")
    long contarActivosSinConductor(@Param("sedeId") UUID sedeId);

    @Query("SELECT m.tipo AS clave, COUNT(m) AS cantidad FROM Movilidad m " +
            "WHERE (:sedeId IS NULL OR m.sede.id = :sedeId) AND m.activo = true GROUP BY m.tipo")
    List<ConteoPorTextoProjection> activosPorTipo(@Param("sedeId") UUID sedeId);
}
