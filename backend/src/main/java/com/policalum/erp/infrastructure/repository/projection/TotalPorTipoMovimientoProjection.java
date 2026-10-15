package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.TipoMovimiento;
import java.math.BigDecimal;

/** Módulo 10: suma de cantidades por tipo de movimiento (entradas / salidas). */
public interface TotalPorTipoMovimientoProjection {
    TipoMovimiento getTipo();
    BigDecimal getCantidad();
    Long getMovimientos();
}
