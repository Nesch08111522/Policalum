package com.policalum.erp.infrastructure.repository.projection;

import java.util.UUID;

/** Módulo 13: cantidad de eventos por usuario. */
public interface ActividadUsuarioProjection {
    UUID getUserId();
    String getNombre();
    String getApellido();
    String getEmail();
    Long getCantidad();
}
