package com.policalum.erp.infrastructure.repository;

import java.util.List;
import java.time.LocalDateTime;
import com.policalum.erp.infrastructure.repository.projection.PagoCxpHistoricoProjection;
import com.policalum.erp.infrastructure.repository.projection.MontoPorSedeProjection;
import com.policalum.erp.infrastructure.repository.projection.CxpPendienteProjection;
import com.policalum.erp.domain.enums.EstadoCxP;
import com.policalum.erp.domain.model.FinanzasCuentaPorPagar;
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
public interface FinanzasCuentaPorPagarRepository extends JpaRepository<FinanzasCuentaPorPagar, UUID> {

    @EntityGraph(attributePaths = {"proveedor", "compra", "sede"})
    @Query("SELECT c FROM FinanzasCuentaPorPagar c WHERE " +
            "(CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId) AND " +
            "(CAST(:proveedorId AS string) IS NULL OR c.proveedor.id = :proveedorId) AND " +
            "(CAST(:estado AS string) IS NULL OR c.estado = :estado) AND " +
            "(CAST(:soloVencidas AS string) IS NULL OR CAST(:soloVencidas AS string) = 'false' OR " +
            " (c.fechaVencimiento IS NOT NULL AND c.fechaVencimiento < CURRENT_DATE " +
            "  AND c.estado <> com.policalum.erp.domain.enums.EstadoCxP.PAGADO))")
    Page<FinanzasCuentaPorPagar> search(
            @Param("sedeId") UUID sedeId,
            @Param("proveedorId") UUID proveedorId,
            @Param("estado") EstadoCxP estado,
            @Param("soloVencidas") Boolean soloVencidas,
            Pageable pageable);

    Optional<FinanzasCuentaPorPagar> findByCompra_Id(UUID compraId);

    long countByEstado(EstadoCxP estado);

    // ============ Módulo 6 ============

    /** FOR UPDATE: dos pagos simultáneos no deben superar el saldo. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM FinanzasCuentaPorPagar c WHERE c.id = :id")
    Optional<FinanzasCuentaPorPagar> findByIdParaActualizar(@Param("id") UUID id);

    /**
     * CxP pagadas por completo ANTES del detalle de abonos (sin filas en finanzas_pagos_cxp):
     * se toma montoPagado en la fecha del pago final.
     */
    @Query("SELECT c.sede.id AS sedeId, c.tipoReferencia AS tipo, c.metodoPago AS metodo, COALESCE(SUM(c.montoPagado), 0) AS total " +
            "FROM FinanzasCuentaPorPagar c WHERE c.estado = com.policalum.erp.domain.enums.EstadoCxP.PAGADO AND " +
            "c.fechaPago >= CAST(:desde AS timestamp) AND c.fechaPago < CAST(:hasta AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId) AND " +
            "NOT EXISTS (SELECT 1 FROM PagoCuentaPorPagar p WHERE p.cuentaPorPagar = c) " +
            "GROUP BY c.sede.id, c.tipoReferencia, c.metodoPago")
    List<PagoCxpHistoricoProjection> pagosSinDetallePorSede(@Param("desde") LocalDateTime desde,
                                                            @Param("hasta") LocalDateTime hasta,
                                                            @Param("sedeId") UUID sedeId);

    /** Saldo pendiente (total - pagado) de las CxP no pagadas, por sede. */
    @Query("SELECT c.sede.id AS sedeId, COALESCE(SUM(c.montoTotal - COALESCE(c.montoPagado, 0)), 0) AS total " +
            "FROM FinanzasCuentaPorPagar c WHERE c.estado <> com.policalum.erp.domain.enums.EstadoCxP.PAGADO AND " +
            "(CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId) GROUP BY c.sede.id")
    List<MontoPorSedeProjection> saldoPendientePorSede(@Param("sedeId") UUID sedeId);

    /** Igual que saldoPendientePorSede pero solo lo vencido antes de :hoy. */
    @Query("SELECT c.sede.id AS sedeId, COALESCE(SUM(c.montoTotal - COALESCE(c.montoPagado, 0)), 0) AS total " +
            "FROM FinanzasCuentaPorPagar c WHERE c.estado <> com.policalum.erp.domain.enums.EstadoCxP.PAGADO AND " +
            "c.fechaVencimiento < CAST(:hoy AS date) AND (CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId) GROUP BY c.sede.id")
    List<MontoPorSedeProjection> saldoVencidoPorSede(@Param("sedeId") UUID sedeId, @Param("hoy") LocalDate hoy);

    /** Saldo pendiente por tipo y fecha de vencimiento (proyección). */
    @Query("SELECT c.tipoReferencia AS tipo, c.fechaVencimiento AS fechaVencimiento, " +
            "COALESCE(SUM(c.montoTotal - COALESCE(c.montoPagado, 0)), 0) AS saldo " +
            "FROM FinanzasCuentaPorPagar c WHERE c.estado <> com.policalum.erp.domain.enums.EstadoCxP.PAGADO AND " +
            "(CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId) GROUP BY c.tipoReferencia, c.fechaVencimiento")
    List<CxpPendienteProjection> pendientesPorVencimiento(@Param("sedeId") UUID sedeId);
}
