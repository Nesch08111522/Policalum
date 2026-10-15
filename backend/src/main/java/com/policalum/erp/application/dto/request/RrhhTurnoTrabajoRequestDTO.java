package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.model.RrhhTurnoTrabajo;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RrhhTurnoTrabajoRequestDTO(
        User user,
        Sede sede,
        String nombreTurno,
        LocalTime horaInicio,
        LocalTime horaFin,
        List<String> diasSemana,
        Boolean activo
) {
}
