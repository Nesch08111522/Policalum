package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Módulo 12: ventas COMPLETADAS agrupadas por producto o servicio (uno de los dos ids es null).
 * cantidad en unidades base (fracción × factor de Kardex); ingresoNeto = subtotal − IGV de la línea.
 */
public interface RentabilidadProjection {
    UUID getProductoId();
    UUID getServicioId();
    String getNombre();
    String getSku();
    String getUnidad();
    String getCategoria();
    BigDecimal getCostoUnitario();
    BigDecimal getCantidad();
    BigDecimal getIngresoNeto();
    Long getVentas();
}
