package com.policalum.erp.application.dto.response;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Turno que aplica a una marcación de ENTRADA. Lo usa RrhhAsistenciaImpl para
 * decidir PRESENTE/TARDANZA y para guardar turnoTrabajoId.
 */
public record TurnoDelDiaDTO(UUID turnoId, String nombreTurno, LocalTime horaInicio, int toleranciaMinutos) {

    /**
     * true si la hora marcada supera inicio + tolerancia. Calcula la diferencia en un
     * rango de ±12 h para que los turnos que empiezan cerca de medianoche no fallen
     * (ej. inicio 23:55, marcación 00:02 = 7 min tarde, no "antes").
     */
    public boolean esTardanza(LocalTime marcacion) {
        return minutosDeDiferencia(horaInicio, marcacion) > toleranciaMinutos;
    }

    public static int minutosDeDiferencia(LocalTime inicio, LocalTime marcacion) {
        int diff = marcacion.toSecondOfDay() / 60 - inicio.toSecondOfDay() / 60;
        if (diff > 720) diff -= 1440;
        if (diff <= -720) diff += 1440;
        return diff;
    }
}
