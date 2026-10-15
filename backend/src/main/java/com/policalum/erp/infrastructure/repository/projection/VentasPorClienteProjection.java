package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Módulo 12: ventas COMPLETADAS por cliente. clienteId null = ventas sin cliente (público general). */
public interface VentasPorClienteProjection {
    UUID getClienteId();
    String getNombre();
    String getTipoDocumento();
    String getNumeroDocumento();
    Long getCantidad();
    BigDecimal getTotal();
    LocalDateTime getPrimeraCompra();
    LocalDateTime getUltimaCompra();
}
