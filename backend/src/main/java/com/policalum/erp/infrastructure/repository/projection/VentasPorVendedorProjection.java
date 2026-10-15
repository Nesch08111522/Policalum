package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface VentasPorVendedorProjection {
    UUID getUserId();
    BigDecimal getNeto();
    Long getCantidad();
}
