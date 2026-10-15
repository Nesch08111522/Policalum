package com.policalum.erp.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Actividad del log en [desde, hasta] (fechas inclusivas).
 * eventosPorDia incluye los días sin eventos (cantidad 0) para graficar directo.
 * logsSinFecha: logs antiguos guardados sin created_at (fuera de cualquier rango).
 */
public record AuditoriaDashboardDTO(
        LocalDate desde,
        LocalDate hasta,
        long totalEventos,
        Map<String, Long> eventosPorAccion,
        List<ConteoDiarioDTO> eventosPorDia,
        List<ActividadUsuarioDTO> topUsuarios,
        List<ActividadTablaDTO> topTablas,
        long eventosDelSistema,
        long logsSinFecha) { }
