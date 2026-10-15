package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * esperado = montoInicial + ventasEfectivo - retirosSocios - reembolsosEfectivo
 */
public record ResumenCierreCajaDTO(UUID turnoId, BigDecimal montoInicial,
                                   BigDecimal ventasEfectivo, BigDecimal retirosSocios,
                                   BigDecimal reembolsosEfectivo,
                                   BigDecimal esperado, BigDecimal declarado,
                                   BigDecimal diferencia) { }
