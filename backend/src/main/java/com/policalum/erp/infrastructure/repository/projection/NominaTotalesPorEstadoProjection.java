package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.EstadoNomina;
import java.math.BigDecimal;

public interface NominaTotalesPorEstadoProjection {
    EstadoNomina getEstado();
    Long getCantidad();
    BigDecimal getSueldos();
    BigDecimal getComisiones();
    BigDecimal getDescuentos();
    BigDecimal getNeto();
}
