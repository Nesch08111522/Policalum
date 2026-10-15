package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.TipoReferenciaCxP;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Módulo 6: cuentas por pagar pagadas antes de existir el detalle de abonos.
 * El método se guardaba como texto (MetodoPago.name()).
 */
public interface PagoCxpHistoricoProjection {
    UUID getSedeId();
    TipoReferenciaCxP getTipo();
    String getMetodo();
    BigDecimal getTotal();
}
