package com.policalum.erp.application.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.policalum.erp.domain.enums.EstadoTurno;
import com.policalum.erp.domain.model.TurnoCaja;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;

import jakarta.validation.constraints.NotNull;

public record TurnoCajaRequestDTO(
        @NotNull(message = "La sede del turno es requerida")
        Sede sede,

        @NotNull(message = "El usuario responsable es requerido")
        User user,

        @NotNull(message = "El monto inicial es requerido")
        BigDecimal montoInicial,

        BigDecimal montoFinalDeclarado,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,

        @NotNull(message = "El estado del turno es requerido")
        EstadoTurno estado,

        String observaciones
) {
}
