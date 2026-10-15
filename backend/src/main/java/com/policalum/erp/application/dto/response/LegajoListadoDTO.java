package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.SistemaPension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LegajoListadoDTO(
        UUID id,
        UUID empleadoId,
        String empleadoNombre,
        String sedeNombre,
        String tipoContrato,
        LocalDate fechaIngreso,
        BigDecimal sueldoBase,
        SistemaPension sistemaPension,
        Boolean activo
) { }
