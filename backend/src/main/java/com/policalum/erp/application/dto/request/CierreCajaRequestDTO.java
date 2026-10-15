package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CierreCajaRequestDTO(@NotNull @DecimalMin("0.00") BigDecimal montoFinalDeclarado,
                                  String observaciones) { }
