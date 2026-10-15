package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.MetodoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PagarNominaRequestDTO(
        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago,

        @Size(max = 100, message = "La referencia no puede superar 100 caracteres")
        String referencia
) { }
