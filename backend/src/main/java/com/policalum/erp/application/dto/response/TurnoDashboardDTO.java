package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @param empleadosSinTurno     empleados con legajo activo que no tienen ningún turno activo
 * @param empleadosSobre48Horas empleados que superan la jornada máxima de 48 h semanales (Perú)
 * @param coberturaPorDia       LUNES..DOMINGO → cantidad de empleados programados ese día
 */
public record TurnoDashboardDTO(
        UUID sedeId,
        long turnosActivos,
        long empleadosConTurno,
        long empleadosSinTurno,
        BigDecimal horasSemanalesProgramadas,
        Map<String, Long> turnosPorNombre,
        Map<String, Long> coberturaPorDia,
        List<String> empleadosSobre48Horas
) { }
