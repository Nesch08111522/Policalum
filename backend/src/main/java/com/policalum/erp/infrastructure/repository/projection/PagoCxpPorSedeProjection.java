package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.TipoReferenciaCxP;
import java.math.BigDecimal;
import java.util.UUID;

/** Módulo 6: abonos a cuentas por pagar por sede, tipo y método. */
public interface PagoCxpPorSedeProjection {
    UUID getSedeId();
    TipoReferenciaCxP getTipo();
    MetodoPago getMetodo();
    BigDecimal getTotal();
}
