package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Proyección de flujo de caja. Además se exige mesesProyeccionDefault ≤ mesesProyeccionMax. */
public record ParametrosFinanzasRequestDTO(
        @NotNull @Min(1) @Max(24) Integer mesesHistoricosProyeccion,
        @NotNull @Min(1) @Max(60) Integer mesesProyeccionDefault,
        @NotNull @Min(1) @Max(60) Integer mesesProyeccionMax) { }
