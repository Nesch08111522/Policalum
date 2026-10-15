package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularRetiroRequestDTO(
        @NotBlank(message = "El motivo de anulación es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) { }
