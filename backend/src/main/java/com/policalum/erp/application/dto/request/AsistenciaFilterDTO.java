package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.EstadoAsistencia;
import com.policalum.erp.domain.enums.TipoMarcacion;

import java.time.LocalDate;
import java.util.UUID;

public record AsistenciaFilterDTO(
        UUID empleadoId,           // Filtrar por empleado específico
        LocalDate fechaInicio,     // Filtrar desde esta fecha
        LocalDate fechaFin,        // Filtrar hasta esta fecha
        TipoMarcacion tipoMarcacion, // Filtrar por tipo (ENTRADA, SALIDA, etc.)
        EstadoAsistencia estado    // Filtrar por estado (PRESENTE, TARDANZA, AUSENTE)
) {}