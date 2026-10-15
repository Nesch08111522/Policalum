package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/** sedeId null = corporativo (cuentas sin sede). */
public record PatrimonioSedeDTO(
        UUID sedeId,
        String sedeNombre,
        BigDecimal disponible,
        BigDecimal inventarioValorizado,
        BigDecimal totalActivos,
        BigDecimal totalPasivos,
        BigDecimal patrimonioNeto) { }
