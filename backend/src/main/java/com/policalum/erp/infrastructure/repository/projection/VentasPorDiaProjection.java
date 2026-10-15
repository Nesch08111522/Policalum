package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Módulo 12: ventas agrupadas por día calendario (semana y mes se arman en el servicio). */
public interface VentasPorDiaProjection {
    LocalDate getFecha();
    Long getCantidad();
    BigDecimal getNeto();
    BigDecimal getIgv();
    BigDecimal getTotal();
}
