package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.policalum.erp.domain.enums.EstadoTurno;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.TurnoCaja;
import com.policalum.erp.domain.model.User;

public record TurnoCajaResponseDTO(
        UUID id,
        Sede sede,
        User user,
        BigDecimal montoInicial,
        BigDecimal montoFinalDeclarado,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,
        EstadoTurno estado,
        String observaciones
) {
}
