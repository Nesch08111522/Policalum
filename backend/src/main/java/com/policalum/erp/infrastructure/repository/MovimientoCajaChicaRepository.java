package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.enums.TipoCajaChica;
import com.policalum.erp.domain.model.MovimientoCajaChica;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoCajaChicaRepository extends JpaRepository<MovimientoCajaChica, UUID> {
    List<MovimientoCajaChica> findByTurno_IdOrderByCreatedAtDesc(UUID turnoId);

    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoCajaChica m WHERE m.turno.id = :turnoId AND m.tipo = :tipo")
    BigDecimal sumar(@Param("turnoId") UUID turnoId, @Param("tipo") TipoCajaChica tipo);
}
