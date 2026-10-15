package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.TipoReferenciaCxP;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Módulo 6: saldo pendiente de cuentas por pagar agrupado por tipo y vencimiento. */
public interface CxpPendienteProjection {
    TipoReferenciaCxP getTipo();
    LocalDate getFechaVencimiento();
    BigDecimal getSaldo();
}
