package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoTurno;
import com.policalum.erp.domain.enums.ResultadoArqueo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Arqueo de un turno (Módulo 12). esperado = inicial + ventas en efectivo − retiros de socios − reembolsos,
 * la misma fórmula del cierre de caja. ventasEfectivo incluye ventas anuladas (como el cierre);
 * efectivoVentasAnuladas muestra cuánto de eso corresponde a ventas anuladas.
 */
public record ArqueoTurnoDTO(
        UUID turnoId,
        String sede,
        String cajero,
        LocalDateTime apertura,
        LocalDateTime cierre,
        EstadoTurno estado,
        BigDecimal montoInicial,
        BigDecimal ventasEfectivo,
        BigDecimal retirosSocios,
        BigDecimal reembolsos,
        BigDecimal esperado,
        BigDecimal declarado,
        BigDecimal diferencia,
        ResultadoArqueo resultado,
        BigDecimal efectivoVentasAnuladas) { }
