package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.SaldoCuentaFinanciera;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaldoCuentaFinancieraRepository extends JpaRepository<SaldoCuentaFinanciera, UUID> {

    /** Corte vigente: el de fecha más reciente (a igual fecha, el último digitado). */
    Optional<SaldoCuentaFinanciera> findFirstByCuenta_IdOrderByFechaCorteDescCreatedAtDesc(UUID cuentaId);

    @EntityGraph(attributePaths = {"cuenta", "registradoPor"})
    Page<SaldoCuentaFinanciera> findByCuenta_IdOrderByFechaCorteDescCreatedAtDesc(UUID cuentaId, Pageable pageable);
}
