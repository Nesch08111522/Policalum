package com.policalum.erp.infrastructure.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Módulo 12: pagos en EFECTIVO por turno de caja. total incluye ventas anuladas, igual que el cierre
 * (TurnoCajaServiceImpl.construirResumen); anuladas se informa aparte.
 */
public interface EfectivoPorTurnoProjection {
    UUID getTurnoId();
    BigDecimal getTotal();
    BigDecimal getAnuladas();
}
