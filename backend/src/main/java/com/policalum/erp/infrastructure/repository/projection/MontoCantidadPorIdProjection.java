package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Suma y conteo agrupados por un id (cliente, proveedor, etc.). */
public interface MontoCantidadPorIdProjection {
    UUID getId();
    BigDecimal getTotal();
    Long getCantidad();
}
