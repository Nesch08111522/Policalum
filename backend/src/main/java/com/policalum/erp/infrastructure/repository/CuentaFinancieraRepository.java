package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.CuentaFinanciera;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaFinancieraRepository extends JpaRepository<CuentaFinanciera, UUID> {

    @EntityGraph(attributePaths = {"sede", "metodosPago"})
    @Query("SELECT c FROM CuentaFinanciera c WHERE c.id = :id")
    Optional<CuentaFinanciera> findDetalleById(@Param("id") UUID id);

    /** Pocas filas (cuentas de la empresa): el filtrado por sede se hace en el servicio. */
    @EntityGraph(attributePaths = {"sede", "metodosPago"})
    List<CuentaFinanciera> findByActivaTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = {"sede", "metodosPago"})
    List<CuentaFinanciera> findAllByOrderByNombreAsc();
}
