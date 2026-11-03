package com.policalum.erp.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record HorarioOperativoRequestDTO(
        @Size(max = 180) String mensaje,
        @NotNull Boolean activo,
        @Valid List<DiaAtencionDTO> dias
) {}
