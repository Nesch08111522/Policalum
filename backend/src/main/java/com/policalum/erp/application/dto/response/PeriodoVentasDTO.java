package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un periodo (día, semana ISO o mes) del reporte de ventas; desde/hasta recortados al rango pedido. */
public record PeriodoVentasDTO(
        String etiqueta,
        LocalDate desde,
        LocalDate hasta,
        long cantidad,
        BigDecimal neto,
        BigDecimal igv,
        BigDecimal total,
        BigDecimal ticketPromedio) { }
