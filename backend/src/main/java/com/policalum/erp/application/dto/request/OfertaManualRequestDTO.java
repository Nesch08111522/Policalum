package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OfertaManualRequestDTO(
        @NotNull Boolean oferta,
        @DecimalMin("0.01") @DecimalMax("99.99") BigDecimal porcentaje,
        @DecimalMin("0.01") BigDecimal precioOferta,
        @Min(1) @Max(365) Integer dias
) {}
