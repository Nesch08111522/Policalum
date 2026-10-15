package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * KPIs de inventario (sedeId null = toda la empresa). Los "Hoy" son del día en curso.
 * valorInventario y valorAjustesHoy solo para ADMIN (null para MULTIFUNCIONAL).
 * alertasBajoMinimo incluye los agotados (cantidad 0 con mínimo configurado).
 */
public record StockDashboardDTO(
        UUID sedeId,
        LocalDate fecha,
        long productosConStock,
        long alertasBajoMinimo,
        long agotados,
        BigDecimal valorInventario,
        long movimientosHoy,
        long entradasHoy,
        long salidasHoy,
        long ajustesHoy,
        BigDecimal valorAjustesHoy,
        long transferenciasHoy,
        List<SaldoKardexDTO> primerasAlertas) { }
