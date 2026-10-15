package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Módulo 10: mermas agrupadas por producto, valorizadas al costo promedio actual. */
public interface MermaProjection {
    UUID getProductoId();
    String getProductoNombre();
    String getCodigoSku();
    String getUnidad();
    Long getMovimientos();
    BigDecimal getCantidad();
    BigDecimal getValor();
}
