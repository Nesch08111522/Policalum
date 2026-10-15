package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** A line is either a product (optionally sold by fraction) or a service. */
public record LineaVentaCommandDTO(
        UUID productoId,
        UUID fraccionId,
        UUID servicioId,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal cantidad) { }
