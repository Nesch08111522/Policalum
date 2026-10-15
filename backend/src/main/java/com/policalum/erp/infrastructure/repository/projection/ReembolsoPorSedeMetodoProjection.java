package com.policalum.erp.infrastructure.repository.projection;

import com.policalum.erp.domain.enums.MetodoReembolso;
import java.math.BigDecimal;
import java.util.UUID;

/** Módulo 6: reembolsos procesados por sede y método. */
public interface ReembolsoPorSedeMetodoProjection {
    UUID getSedeId();
    MetodoReembolso getMetodo();
    BigDecimal getTotal();
}
