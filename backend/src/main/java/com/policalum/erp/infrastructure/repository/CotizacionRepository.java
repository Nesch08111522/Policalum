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
    @Query("select c from Cotizacion c where (CAST(:clienteId AS string) IS NULL or c.cliente.id = :clienteId) and (CAST(:sedeId AS string) IS NULL or c.sede.id = :sedeId) and (CAST(:estado AS string) IS NULL or c.estado = :estado) and (CAST(:desde AS string) IS NULL or c.createdAt >= CAST(CAST(:desde AS string) AS timestamp)) and (CAST(:hasta AS string) IS NULL or c.createdAt <= CAST(CAST(:hasta AS string) AS timestamp))")
    Page<Cotizacion> buscar(@Param("clienteId") UUID clienteId, @Param("sedeId") UUID sedeId,
                            @Param("estado") EstadoCotizacion estado, @Param("desde") LocalDateTime desde,
                            @Param("hasta") LocalDateTime hasta, Pageable pageable);

    @EntityGraph(attributePaths = {"sede", "user", "cliente", "venta"})
    List<Cotizacion> findByEstadoAndFechaVigenciaBefore(EstadoCotizacion estado, LocalDate fecha);

    long countByEstado(EstadoCotizacion estado);

    // ============ Módulo 6: proyección ============

    /** Total de cotizaciones aún vigentes (ingreso potencial, no comprometido). */
    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Cotizacion c WHERE c.estado IN :estados AND " +
            "(c.fechaVigencia IS NULL OR c.fechaVigencia >= CAST(:hoy AS date)) AND (CAST(:sedeId AS string) IS NULL OR c.sede.id = :sedeId)")
    BigDecimal sumarVigentes(@Param("estados") Collection<EstadoCotizacion> estados,
                             @Param("hoy") LocalDate hoy,
                             @Param("sedeId") UUID sedeId);
}
