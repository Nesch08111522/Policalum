package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Módulo 12: devoluciones APROBADAS por producto (unidades base y monto neto sin IGV). */
public interface DevueltoPorProductoProjection {
    UUID getProductoId();
    BigDecimal getCantidad();
    BigDecimal getMontoNeto();
}
