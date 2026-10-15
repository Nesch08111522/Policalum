package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;

/** Módulo 6: suma y conteo. */
public interface MontoCantidadProjection {
    BigDecimal getTotal();
    Long getCantidad();
}
