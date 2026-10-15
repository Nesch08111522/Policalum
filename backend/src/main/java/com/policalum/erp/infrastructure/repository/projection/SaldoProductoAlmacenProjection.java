package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Módulo 10: saldo de un producto en un almacén. */
public interface SaldoProductoAlmacenProjection {
    UUID getProductoId();
    String getProductoNombre();
    String getCodigoSku();
    String getUnidad();
    UUID getAlmacenId();
    String getAlmacenNombre();
    UUID getSedeId();
    String getSedeNombre();
    BigDecimal getCantidad();
    BigDecimal getStockMinimo();
    LocalDateTime getUltimoMovimiento();
}
