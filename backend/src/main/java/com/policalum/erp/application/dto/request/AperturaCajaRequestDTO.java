package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record AperturaCajaRequestDTO(@NotNull UUID sedeId,
                                    @NotNull @DecimalMin("0.00") BigDecimal montoInicial,
                                    String observaciones) { }
