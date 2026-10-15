package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoRetiro;
import com.policalum.erp.domain.enums.OrigenFondos;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RetiroSocioListadoDTO(
        UUID id,
        String adminNombre,
        String sedeNombre,
        BigDecimal montoRetirado,
        OrigenFondos origenFondos,
        EstadoRetiro estado,
        LocalDateTime fechaRetiro
) { }
