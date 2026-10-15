package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.DetalleCotizacion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetalleCotizacionRepository extends JpaRepository<DetalleCotizacion, UUID> {
    @EntityGraph(attributePaths = {"producto", "fraccion", "servicio"})
    List<DetalleCotizacion> findByCotizacion_Id(UUID cotizacionId);
    @Modifying
    @Query("DELETE FROM DetalleCotizacion d WHERE d.cotizacion.id = :cotizacionId")
    void deleteByCotizacion_Id(@Param("cotizacionId") UUID cotizacionId);
}
