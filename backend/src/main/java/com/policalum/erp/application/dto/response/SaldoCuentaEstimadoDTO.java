package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.TipoCuentaFinanciera;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * saldoEstimado = saldoCorte + movimientosPosteriores (cobros - pagos de los métodos de la
 * cuenta desde la fecha de corte). null si la cuenta aún no tiene saldo registrado.
 */
public record SaldoCuentaEstimadoDTO(
        UUID cuentaId,
        String nombre,
        TipoCuentaFinanciera tipo,
        UUID sedeId,
        String sedeNombre,
        List<MetodoPago> metodosPago,
        BigDecimal saldoCorte,
        LocalDateTime fechaCorte,
        BigDecimal movimientosPosteriores,
        BigDecimal saldoEstimado,
        Long diasDesdeCorte) { }
