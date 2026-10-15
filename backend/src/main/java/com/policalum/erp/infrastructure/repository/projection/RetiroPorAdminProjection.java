package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface RetiroPorAdminProjection {
    UUID getAdminId();
    String getNombre();
    String getApellido();
    Long getCantidad();
    BigDecimal getTotal();
}
