package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PagoCxPRequestDTO(
        @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
        @NotNull MetodoPago metodoPago,
        @Size(max = 100) String referencia,
        @Size(max = 500) String observaciones
) { }