package com.policalum.erp.infrastructure.repository.projection;

import java.time.LocalDate;

/** Módulo 13: cantidad de eventos por día. */
public interface ConteoPorFechaProjection {
    LocalDate getFecha();
    Long getCantidad();
}
