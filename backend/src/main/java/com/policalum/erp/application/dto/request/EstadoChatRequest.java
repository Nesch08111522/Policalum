package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EstadoChatRequest(
        @NotBlank @Pattern(regexp = "ABIERTA|EN_ESPERA|CERRADA") String estado
) {}
