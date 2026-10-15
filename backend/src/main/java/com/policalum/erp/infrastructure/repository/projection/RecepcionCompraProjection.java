package com.policalum.erp.infrastructure.repository.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Módulo 12: fecha de emisión y de recepción (primera entrada en Kardex) de una compra recibida. */
public interface RecepcionCompraProjection {
    UUID getProveedorId();
    LocalDate getFechaEmision();
    LocalDateTime getFechaRecepcion();
}
