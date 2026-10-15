package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.MetodoPago;
import java.math.BigDecimal;

/** Módulo 12: cobros agrupados por método de pago. */
public interface MontoPorMetodoProjection {
    MetodoPago getMetodo();
    Long getCantidad();
    BigDecimal getTotal();
}
