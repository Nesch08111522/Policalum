package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Módulo 11: stock de un producto sumado en los almacenes filtrados. */
public interface StockConsolidadoProjection {
    UUID getProductoId();
    String getProductoNombre();
    String getCodigoSku();
    String getUnidad();
    BigDecimal getStockTotal();
    BigDecimal getStockMinimoTotal();
    Long getAlmacenes();
    Long getAlmacenesBajoMinimo();
}
