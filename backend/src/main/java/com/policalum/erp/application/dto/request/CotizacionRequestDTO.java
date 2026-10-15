package com.policalum.erp.application.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CotizacionRequestDTO(
        @NotNull UUID sedeId,
        UUID clienteId,
        LocalDate fechaVigencia,
        @Size(max = 2000) String observaciones,
        @NotEmpty List<@Valid DetalleCotizacionCommandDTO> lineas
) {
}
