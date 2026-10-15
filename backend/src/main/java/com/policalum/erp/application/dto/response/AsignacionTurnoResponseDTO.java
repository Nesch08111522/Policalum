package com.policalum.erp.application.dto.response;

import java.util.List;

public record AsignacionTurnoResponseDTO(
        int creados,
        List<TurnoTrabajoResponseDTO> turnos
) { }
