package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HorarioOperativoRequestDTO(
        @Size(max = 180) String mensaje,
        @NotNull Boolean activo
) {}
