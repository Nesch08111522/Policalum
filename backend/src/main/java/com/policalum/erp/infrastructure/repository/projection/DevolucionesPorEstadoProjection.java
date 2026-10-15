package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.EstadoDevolucion;
import java.math.BigDecimal;

public interface DevolucionesPorEstadoProjection {
    EstadoDevolucion getEstado();
    Long getCantidad();
    BigDecimal getMonto();
}
