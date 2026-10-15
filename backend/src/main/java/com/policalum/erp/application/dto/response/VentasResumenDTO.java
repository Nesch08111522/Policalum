package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.AgrupacionReporte;
import com.policalum.erp.domain.enums.EstadoVenta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Resumen del reporte de ventas (Módulo 12). Montos en PEN; total y ticket con IGV.
 * devoluciones/totalMenosDevoluciones son null cuando hay filtros que las devoluciones no admiten.
 */
public record VentasResumenDTO(
        LocalDate desde,
        LocalDate hasta,
        AgrupacionReporte agrupacion,
        UUID sedeId,
        EstadoVenta estado,
        long cantidad,
        BigDecimal neto,
        BigDecimal igv,
        BigDecimal total,
        BigDecimal ticketPromedio,
        BigDecimal devoluciones,
        Long cantidadDevoluciones,
        BigDecimal totalMenosDevoluciones,
        List<PeriodoVentasDTO> periodos,
        List<MetodoPagoMontoDTO> porMetodoPago,
        List<String> advertencias) { }
