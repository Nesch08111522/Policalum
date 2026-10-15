package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * @param sedeId            opcional: si es null se usa la sede del empleado
 * @param horaFin           si es menor que horaInicio, el turno es nocturno (termina al día siguiente)
 * @param diasSemana        LUNES..DOMINGO (con o sin tilde, mayúsculas o minúsculas)
 * @param toleranciaMinutos opcional, 0 a 120 (null = 10)
 * @param activo            opcional; null = true al crear y "sin cambio" al actualizar
 */
public record TurnoTrabajoRequestDTO(
        @NotNull(message = "El empleado es obligatorio")
        UUID userId,

        UUID sedeId,

        @NotBlank(message = "El nombre del turno es obligatorio")
        @Size(max = 50, message = "El nombre del turno no puede superar 50 caracteres")
        String nombreTurno,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        @NotEmpty(message = "Debe indicar al menos un día de la semana")
        List<String> diasSemana,

        @Min(value = 0, message = "La tolerancia no puede ser negativa")
        @Max(value = 120, message = "La tolerancia no puede superar 120 minutos")
        Integer toleranciaMinutos,

        Boolean activo
) { }
