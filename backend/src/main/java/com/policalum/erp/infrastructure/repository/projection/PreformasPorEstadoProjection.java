package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.EstadoPreforma;
import java.math.BigDecimal;

public interface PreformasPorEstadoProjection {
    EstadoPreforma getEstado();
    Long getCantidad();
    BigDecimal getMonto();
}
