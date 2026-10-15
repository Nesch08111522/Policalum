package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Proyección: ingresos por tendencia (promedio de los últimos meses completos) y egresos
 * por el mayor entre tendencia y documentos comprometidos.
 * ingresosPotenciales (cotizaciones vigentes + preformas abiertas) es informativo: NO entra
 * en el flujo proyectado.
 */
public record ProyeccionFlujoResponseDTO(
        UUID sedeId,
        LocalDate fechaBase,
        int mesesHistoricosUsados,
        BigDecimal saldoInicial,
        BigDecimal promedioIngresos,
        BigDecimal promedioEgresos,
        BigDecimal promedioRetiros,
        List<MesProyectadoDTO> meses,
        BigDecimal ingresosPotenciales,
        List<String> advertencias) { }
