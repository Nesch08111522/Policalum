package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.MetodoPago;
import java.math.BigDecimal;
import java.util.UUID;

/** Módulo 12: pagos de una venta por método (para listar "EFECTIVO 50 + YAPE 30"). */
public interface PagoPorVentaProjection {
    UUID getVentaId();
    MetodoPago getMetodo();
    BigDecimal getTotal();
}
