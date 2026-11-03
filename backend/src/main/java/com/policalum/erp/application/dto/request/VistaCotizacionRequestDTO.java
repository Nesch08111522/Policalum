package com.policalum.erp.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VistaCotizacionRequestDTO(
        @NotBlank @Size(max = 80) String distrito,
        @NotEmpty @Size(max = 40) List<@Valid DetalleCotizacionCommandDTO> lineas
) {}
