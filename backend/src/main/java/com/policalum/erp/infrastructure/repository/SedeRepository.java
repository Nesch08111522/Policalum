package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface SedeRepository extends JpaRepository< Sede, UUID> {

    Page<Sede> findByActiva(Boolean activa, Pageable pageable);

    /**
     * La sede tiene almacenes, usuarios o documentos.
     * En ese caso no se borra: se desactiva para conservar el historial.
     */
    @Query("""
            SELECT CASE WHEN
                EXISTS (SELECT 1 FROM Almacen a WHERE a.sede.id = :id)
                OR EXISTS (SELECT 1 FROM User u WHERE u.sede.id = :id)
                OR EXISTS (SELECT 1 FROM Venta v WHERE v.sede.id = :id)
                OR EXISTS (SELECT 1 FROM Compra c WHERE c.sede.id = :id)
                OR EXISTS (SELECT 1 FROM Cotizacion c WHERE c.sede.id = :id)
                OR EXISTS (SELECT 1 FROM PreformaVenta p WHERE p.sede.id = :id)
                OR EXISTS (SELECT 1 FROM Devolucion d WHERE d.sede.id = :id)
                OR EXISTS (SELECT 1 FROM FinanzasCuentaPorPagar x WHERE x.sede.id = :id)
                OR EXISTS (SELECT 1 FROM FinanzasRetiroSocio r WHERE r.sede.id = :id)
                OR EXISTS (SELECT 1 FROM Movilidad m WHERE m.sede.id = :id)
                OR EXISTS (SELECT 1 FROM OrdenDespacho o WHERE o.sede.id = :id)
                OR EXISTS (SELECT 1 FROM CuentaFinanciera f WHERE f.sede.id = :id)
                OR EXISTS (SELECT 1 FROM TurnoCaja t WHERE t.sede.id = :id)
                OR EXISTS (SELECT 1 FROM SerieComprobante s WHERE s.sede.id = :id)
                OR EXISTS (SELECT 1 FROM RrhhNomina n WHERE n.sede.id = :id)
                OR EXISTS (SELECT 1 FROM RrhhComision c WHERE c.sede.id = :id)
                OR EXISTS (SELECT 1 FROM RrhhTurnoTrabajo t WHERE t.sede.id = :id)
            THEN TRUE ELSE FALSE END
            """)
    boolean tieneHistorial(@Param("id") UUID id);
}
