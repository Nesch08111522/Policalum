package com.policalum.erp.infrastructure.repository;

import java.util.Optional;
import com.policalum.erp.infrastructure.repository.projection.ConteoPorTablaProjection;
import com.policalum.erp.infrastructure.repository.projection.ConteoPorFechaProjection;
import com.policalum.erp.infrastructure.repository.projection.ConteoPorAccionProjection;
import com.policalum.erp.infrastructure.repository.projection.ActividadUsuarioProjection;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(CAST(:tabla AS string) IS NULL OR a.tabla = :tabla) AND " +
            "(CAST(:registroId AS string) IS NULL OR a.registroId = :registroId) AND " +
            "(CAST(:userId AS string) IS NULL OR a.user.id = :userId) AND " +
            "(CAST(:accion AS string) IS NULL OR a.accion = :accion) AND " +
            "a.createdAt >= CAST(:desde AS timestamp) AND " +
            "a.createdAt <= CAST(:hasta AS timestamp)")
    Page<AuditLog> search(
            @Param("tabla") String tabla,
            @Param("registroId") UUID registroId,
            @Param("userId") UUID userId,
            @Param("accion") AccionAudit accion,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<AuditLog> findByTablaAndRegistroIdOrderByCreatedAtDesc(String tabla, UUID registroId);

    // ============ Módulo 13: consulta de auditoría ============
    // Fechas siempre no nulas, rango semiabierto [desde, hasta). incluirSinFecha = true agrega
    // los logs antiguos guardados con created_at NULL (antes del fix del Módulo 13).

    String FILTROS_AUDITORIA = "(CAST(:tabla AS string) IS NULL OR a.tabla = :tabla) AND " +
            "(CAST(:registroId AS string) IS NULL OR a.registroId = :registroId) AND " +
            "(CAST(:userId AS string) IS NULL OR a.user.id = :userId) AND " +
            "(CAST(:accion AS string) IS NULL OR a.accion = :accion) AND " +
            "((a.createdAt >= CAST(:desde AS timestamp) AND a.createdAt < CAST(:hasta AS timestamp)) OR (:incluirSinFecha = true AND a.createdAt IS NULL))";

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT a FROM AuditLog a WHERE a.id = :id")
    Optional<AuditLog> findDetalleById(@Param("id") UUID id);

    /** Más recientes primero; los logs sin fecha al final. */
    @EntityGraph(attributePaths = {"user"})
    @Query(value = "SELECT a FROM AuditLog a WHERE " + FILTROS_AUDITORIA + " ORDER BY a.createdAt DESC NULLS LAST, a.id DESC",
            countQuery = "SELECT COUNT(a) FROM AuditLog a WHERE " + FILTROS_AUDITORIA)
    Page<AuditLog> buscar(@Param("tabla") String tabla,
                          @Param("registroId") UUID registroId,
                          @Param("userId") UUID userId,
                          @Param("accion") AccionAudit accion,
                          @Param("desde") LocalDateTime desde,
                          @Param("hasta") LocalDateTime hasta,
                          @Param("incluirSinFecha") boolean incluirSinFecha,
                          Pageable pageable);

    /**
     * Para exportar por lotes: orden ascendente estable (fecha, id), así los logs que se
     * registren mientras se exporta van al final y no desplazan las páginas ya leídas.
     */
    @EntityGraph(attributePaths = {"user"})
    @Query(value = "SELECT a FROM AuditLog a WHERE " + FILTROS_AUDITORIA + " ORDER BY a.createdAt ASC NULLS FIRST, a.id ASC",
            countQuery = "SELECT COUNT(a) FROM AuditLog a WHERE " + FILTROS_AUDITORIA)
    Page<AuditLog> buscarParaExportar(@Param("tabla") String tabla,
                                      @Param("registroId") UUID registroId,
                                      @Param("userId") UUID userId,
                                      @Param("accion") AccionAudit accion,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta,
                                      @Param("incluirSinFecha") boolean incluirSinFecha,
                                      Pageable pageable);

    @Query("SELECT a.accion AS accion, COUNT(a) AS cantidad FROM AuditLog a " +
            "WHERE a.createdAt >= CAST(:desde AS timestamp) AND a.createdAt < CAST(:hasta AS timestamp) GROUP BY a.accion")
    List<ConteoPorAccionProjection> conteoPorAccion(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT CAST(a.createdAt AS LocalDate) AS fecha, COUNT(a) AS cantidad FROM AuditLog a " +
            "WHERE a.createdAt >= CAST(:desde AS timestamp) AND a.createdAt < CAST(:hasta AS timestamp) GROUP BY CAST(a.createdAt AS LocalDate)")
    List<ConteoPorFechaProjection> conteoPorDia(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT a.tabla AS tabla, COUNT(a) AS cantidad FROM AuditLog a " +
            "WHERE a.createdAt >= CAST(:desde AS timestamp) AND a.createdAt < CAST(:hasta AS timestamp) GROUP BY a.tabla ORDER BY COUNT(a) DESC, a.tabla")
    List<ConteoPorTablaProjection> topTablas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                             Pageable limite);

    @Query("SELECT u.id AS userId, u.nombre AS nombre, u.apellido AS apellido, u.email AS email, COUNT(a) AS cantidad " +
            "FROM AuditLog a JOIN a.user u WHERE a.createdAt >= CAST(:desde AS timestamp) AND a.createdAt < CAST(:hasta AS timestamp) " +
            "GROUP BY u.id, u.nombre, u.apellido, u.email ORDER BY COUNT(a) DESC, u.nombre")
    List<ActividadUsuarioProjection> topUsuarios(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                                 Pageable limite);

    /** Eventos del sistema (sin usuario autenticado: tareas programadas, login fallido...). */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.user IS NULL AND a.createdAt >= CAST(:desde AS timestamp) AND a.createdAt < CAST(:hasta AS timestamp)")
    long contarSinUsuario(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** Logs anteriores al fix del Módulo 13 (created_at NULL). */
    long countByCreatedAtIsNull();
}
