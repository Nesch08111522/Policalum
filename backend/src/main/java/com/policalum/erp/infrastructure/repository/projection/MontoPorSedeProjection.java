package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Módulo 6: un monto agrupado por sede (inventario, deudas, pendientes). */
public interface MontoPorSedeProjection {
    UUID getSedeId();
    BigDecimal getTotal();
}
