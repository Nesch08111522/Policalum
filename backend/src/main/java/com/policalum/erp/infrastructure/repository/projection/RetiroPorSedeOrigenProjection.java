package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.OrigenFondos;
import java.math.BigDecimal;
import java.util.UUID;

/** Módulo 6: retiros de socios REGISTRADOS por sede y origen de fondos. */
public interface RetiroPorSedeOrigenProjection {
    UUID getSedeId();
    OrigenFondos getOrigen();
    BigDecimal getTotal();
}
