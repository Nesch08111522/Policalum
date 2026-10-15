package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Exactly one of productoId or servicioId is required. */
public record DetalleCotizacionCommandDTO(UUID productoId, UUID fraccionId, UUID servicioId,
                                          @NotNull @DecimalMin("0.0001") BigDecimal cantidad) { }
