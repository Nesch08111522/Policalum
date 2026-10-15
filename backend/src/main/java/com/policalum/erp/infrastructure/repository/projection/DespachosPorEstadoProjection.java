package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.EstadoDespacho;

public interface DespachosPorEstadoProjection {
    EstadoDespacho getEstado();
    Long getCantidad();
}
