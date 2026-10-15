package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Módulo 10: existencias de un producto sumadas en los almacenes filtrados, con su costo promedio. */
public interface ValorizacionProductoProjection {
    UUID getProductoId();
    String getProductoNombre();
    String getCodigoSku();
    String getUnidad();
    BigDecimal getCostoUnitario();
    BigDecimal getCantidad();
    Long getAlmacenes();
}
