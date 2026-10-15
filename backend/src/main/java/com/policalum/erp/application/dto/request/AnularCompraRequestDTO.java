package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularCompraRequestDTO(
        @NotBlank @Size(max = 500) String motivo
) { }