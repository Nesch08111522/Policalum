package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.GastoEmpresarial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GastoEmpresarialRepository extends JpaRepository<GastoEmpresarial, UUID> {

    @Query(value = "SELECT g FROM GastoEmpresarial g WHERE g.anulado = false AND g.fecha >= :desde AND g.fecha <= :hasta " +
            "AND (CAST(:sedeId AS string) IS NULL OR g.sede.id = :sedeId)",
            countQuery = "SELECT COUNT(g) FROM GastoEmpresarial g WHERE g.anulado = false AND g.fecha >= :desde AND g.fecha <= :hasta " +
                    "AND (CAST(:sedeId AS string) IS NULL OR g.sede.id = :sedeId)")
    Page<GastoEmpresarial> buscar(@Param("sedeId") UUID sedeId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, Pageable pageable);

    @Query("SELECT COALESCE(SUM(g.monto), 0) FROM GastoEmpresarial g WHERE g.anulado = false AND g.fecha >= :desde AND g.fecha <= :hasta " +
            "AND (CAST(:sedeId AS string) IS NULL OR g.sede.id = :sedeId)")
    BigDecimal sumar(@Param("sedeId") UUID sedeId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT COUNT(g) FROM GastoEmpresarial g WHERE g.anulado = false AND g.fecha >= :desde AND g.fecha <= :hasta " +
            "AND (CAST(:sedeId AS string) IS NULL OR g.sede.id = :sedeId)")
    long contar(@Param("sedeId") UUID sedeId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
