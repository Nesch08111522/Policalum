package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Vigencias de ventas. Además se exige preformaMinutosMin ≤ preformaMinutosDefault ≤ preformaMinutosMax. */
public record ParametrosVentasRequestDTO(
        @NotNull @Min(1) @Max(365) Integer vigenciaCotizacionDias,
        @NotNull @Min(1) @Max(43_200) Integer preformaMinutosDefault,
        @NotNull @Min(1) @Max(1_440) Integer preformaMinutosMin,
        @NotNull @Min(1) @Max(43_200) Integer preformaMinutosMax) { }
