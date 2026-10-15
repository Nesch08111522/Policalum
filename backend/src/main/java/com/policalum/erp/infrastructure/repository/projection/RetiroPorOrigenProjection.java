package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.OrigenFondos;
import java.math.BigDecimal;

public interface RetiroPorOrigenProjection {
    OrigenFondos getOrigen();
    Long getCantidad();
    BigDecimal getTotal();
}
