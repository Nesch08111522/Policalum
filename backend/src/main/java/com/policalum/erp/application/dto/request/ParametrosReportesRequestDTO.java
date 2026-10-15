package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Tope de filas de las exportaciones CSV (protege memoria y tiempo de respuesta). */
public record ParametrosReportesRequestDTO(
        @NotNull @Min(1_000) @Max(500_000) Integer maxFilasExportacion) { }
