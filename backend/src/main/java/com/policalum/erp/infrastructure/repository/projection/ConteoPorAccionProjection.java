package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.AccionAudit;

/** Módulo 13: cantidad de eventos por acción. */
public interface ConteoPorAccionProjection {
    AccionAudit getAccion();
    Long getCantidad();
}
