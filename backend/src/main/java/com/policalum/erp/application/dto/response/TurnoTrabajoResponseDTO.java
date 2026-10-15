package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TurnoTrabajoResponseDTO(
        UUID id,
        UUID userId,
        String empleadoNombre,
        UUID sedeId,
        String sedeNombre,
        String nombreTurno,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean nocturno,
        BigDecimal duracionHoras,
        List<String> diasSemana,
        BigDecimal horasSemanales,
        Integer toleranciaMinutos,
        Boolean activo,
        LocalDateTime createdAt
) { }
