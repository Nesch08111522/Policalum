package com.policalum.erp.infrastructure.repository;

import java.util.Collection;
import java.math.BigDecimal;
import com.policalum.erp.domain.enums.EstadoCotizacion;
import com.policalum.erp.domain.model.Cotizacion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CotizacionRepository extends JpaRepository<Cotizacion, UUID> {
    @EntityGraph(attributePaths = {"sede", "user", "cliente", "venta"})
    @Query("select c from Cotizacion c where (:clienteId is null or c.cliente.id = :clienteId) and (:sedeId is null or c.sede.id = :sedeId) and (:estado is null or c.estado = :estado) and c.createdAt >= :desde and c.createdAt <= :hasta")
    Page<Cotizacion> buscar(@Param("clienteId") UUID clienteId, @Param("sedeId") UUID sedeId,
                            @Param("estado") EstadoCotizacion estado, @Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta, Pageable pageable);

    @EntityGraph(attributePaths = {"sede", "user", "cliente", "venta"})
    List<Cotizacion> findByEstadoAndFechaVigenciaBefore(EstadoCotizacion estado, LocalDate fecha);

    long countByEstado(EstadoCotizacion estado);

    // ============ Módulo 6: proyección ============

    /** Total de cotizaciones aún vigentes (ingreso potencial, no comprometido). */
    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Cotizacion c WHERE c.estado IN :estados AND " +
            "(c.fechaVigencia IS NULL OR c.fechaVigencia >= :hoy) AND (:sedeId IS NULL OR c.sede.id = :sedeId)")
    BigDecimal sumarVigentes(@Param("estados") Collection<EstadoCotizacion> estados,
                             @Param("hoy") LocalDate hoy,
                             @Param("sedeId") UUID sedeId);
}
