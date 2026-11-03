package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatTextoRequest(
        @NotBlank @Size(max = 2000) String texto,
        @Size(max = 120) String autorNombre
) {}
