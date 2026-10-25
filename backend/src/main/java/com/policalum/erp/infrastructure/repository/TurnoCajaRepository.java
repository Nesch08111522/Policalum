package com.policalum.erp.infrastructure.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import com.policalum.erp.domain.enums.EstadoTurno;
import com.policalum.erp.domain.model.TurnoCaja;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, UUID> {
    Optional<TurnoCaja> findByUser_IdAndSede_IdAndEstado(UUID userId, UUID sedeId, EstadoTurno estado);

    @Query("SELECT t FROM TurnoCaja t JOIN FETCH t.sede JOIN FETCH t.user WHERE t.user.id = :userId AND t.estado = com.policalum.erp.domain.enums.EstadoTurno.ABIERTO ORDER BY t.fechaApertura DESC")
    java.util.List<TurnoCaja> abiertosDe(@Param("userId") UUID userId);

    /**
     * Igual que findByUser_IdAndSede_IdAndEstado pero con SELECT ... FOR UPDATE.
     * Evita que dos retiros simultáneos pisen totalRetirosSocios.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TurnoCaja t WHERE t.user.id = :userId AND t.sede.id = :sedeId AND t.estado = :estado")
    Optional<TurnoCaja> findParaActualizar(@Param("userId") UUID userId,
                                           @Param("sedeId") UUID sedeId,
                                           @Param("estado") EstadoTurno estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TurnoCaja t WHERE t.id = :id")
    Optional<TurnoCaja> findByIdParaActualizar(@Param("id") UUID id);

    // ============ Módulo 12: reportes ============

    /** Turnos abiertos en [desde, hasta) para el arqueo, más recientes primero, con sede y cajero cargados. */
    @Query(value = "SELECT t FROM TurnoCaja t JOIN FETCH t.sede JOIN FETCH t.user " +
            "WHERE t.fechaApertura >= CAST(:desde AS timestamp) AND t.fechaApertura < CAST(:hasta AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR t.sede.id = :sedeId) " +
            "AND (CAST(:userId AS string) IS NULL OR t.user.id = :userId) AND (CAST(:estado AS string) IS NULL OR t.estado = :estado) " +
            "ORDER BY t.fechaApertura DESC, t.id",
            countQuery = "SELECT COUNT(t) FROM TurnoCaja t " +
            "WHERE t.fechaApertura >= CAST(:desde AS timestamp) AND t.fechaApertura < CAST(:hasta AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR t.sede.id = :sedeId) " +
            "AND (CAST(:userId AS string) IS NULL OR t.user.id = :userId) AND (CAST(:estado AS string) IS NULL OR t.estado = :estado)")
    Page<TurnoCaja> reporteArqueos(@Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta,
                                   @Param("sedeId") UUID sedeId,
                                   @Param("userId") UUID userId,
                                   @Param("estado") EstadoTurno estado,
                                   Pageable pageable);
}
