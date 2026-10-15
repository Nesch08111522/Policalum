package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Módulo 12: compras por proveedor según fecha de emisión. monto y recibidasMonedaBase solo cuentan
 * compras RECIBIDAS en la moneda pedida (PEN); las otras se informan aparte.
 */
public interface ComprasPorProveedorProjection {
    UUID getProveedorId();
    String getNombre();
    String getNumeroDocumento();
    Long getCantidad();
    Long getRecibidas();
    Long getPendientes();
    Long getAnuladas();
    Long getRecibidasMonedaBase();
    BigDecimal getMonto();
    LocalDate getUltimaCompra();
}
