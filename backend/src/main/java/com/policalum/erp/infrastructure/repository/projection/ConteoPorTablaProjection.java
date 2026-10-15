package com.policalum.erp.infrastructure.repository.projection;

/** Módulo 13: cantidad de eventos por tabla auditada. */
public interface ConteoPorTablaProjection {
    String getTabla();
    Long getCantidad();
}
