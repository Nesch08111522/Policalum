package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarDevolucionRequestDTO(
        @NotBlank(message = "El motivo del rechazo es obligatorio")
        @Size(max = 1000, message = "El motivo no puede superar 1000 caracteres")
        String motivo
) { }
