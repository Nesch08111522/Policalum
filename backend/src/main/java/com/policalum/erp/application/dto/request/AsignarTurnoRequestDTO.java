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
 * Crea el mismo horario para varios empleados de una vez (todo o nada).
 *
 * @param sedeId opcional: si es null, cada turno toma la sede de su empleado
 */
public record AsignarTurnoRequestDTO(
        @NotEmpty(message = "Debe indicar al menos un empleado")
        @Size(max = 100, message = "Máximo 100 empleados por asignación")
        List<UUID> userIds,

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
        Integer toleranciaMinutos
) { }
