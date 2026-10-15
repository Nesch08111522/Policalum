package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.MetodoPago;
import java.math.BigDecimal;
import java.util.UUID;

/** Módulo 6: suma de dinero agrupada por sede y método de pago (cobros de ventas, nóminas). */
public interface MontoPorSedeMetodoProjection {
    UUID getSedeId();
    MetodoPago getMetodo();
    BigDecimal getTotal();
}
