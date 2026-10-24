package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.PagoCuentaPorPagar;
import com.policalum.erp.infrastructure.repository.projection.PagoCxpPorSedeProjection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoCuentaPorPagarRepository extends JpaRepository<PagoCuentaPorPagar, UUID> {

    List<PagoCuentaPorPagar> findByCuentaPorPagar_IdOrderByFechaPagoAsc(UUID cuentaPorPagarId);

    /** Abonos en [desde, hasta) agrupados por sede, tipo de CxP y método. */
    @Query("SELECT p.cuentaPorPagar.sede.id AS sedeId, p.cuentaPorPagar.tipoReferencia AS tipo, " +
            "p.metodoPago AS metodo, COALESCE(SUM(p.monto), 0) AS total " +
            "FROM PagoCuentaPorPagar p WHERE p.fechaPago >= CAST(:desde AS timestamp) AND p.fechaPago < CAST(:hasta AS timestamp) AND " +
            "(CAST(:sedeId AS string) IS NULL OR p.cuentaPorPagar.sede.id = :sedeId) " +
            "GROUP BY p.cuentaPorPagar.sede.id, p.cuentaPorPagar.tipoReferencia, p.metodoPago")
    List<PagoCxpPorSedeProjection> pagosPorSede(@Param("desde") LocalDateTime desde,
                                                @Param("hasta") LocalDateTime hasta,
                                                @Param("sedeId") UUID sedeId);
}
