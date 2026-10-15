package com.policalum.erp.application.dto.response;

import java.util.List;

public record DashboardAsistenciaDTO(
        Long totalEmpleados,
        Long presentesHoy,
        Long ausentesHoy,
        Long tardanzasHoy,
        List<RrhhAsistenciaResponseDTO> asistenciasRecientes
) {}