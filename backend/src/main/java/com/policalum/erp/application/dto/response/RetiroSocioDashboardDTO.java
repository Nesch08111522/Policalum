package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Resumen de retiros REGISTRADOS (los anulados solo se cuentan). */
public record RetiroSocioDashboardDTO(
        UUID sedeId,
        LocalDate desde,
        LocalDate hasta,
        BigDecimal totalRetirado,
        long cantidadRetiros,
        long cantidadAnulados,
        BigDecimal totalCajaFisica,
        BigDecimal totalCuentaBancaria,
        List<RetiroPorAdminDTO> porAdmin
) { }
