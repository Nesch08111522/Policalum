package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;

/** Totales agregados de ventas o compras en un periodo. */
public interface TotalesFinancierosProjection {
    BigDecimal getNeto();
    BigDecimal getIgv();
    BigDecimal getTotal();
    Long getCantidad();
}
