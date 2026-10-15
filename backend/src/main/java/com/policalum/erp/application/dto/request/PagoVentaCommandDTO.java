package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PagoVentaCommandDTO(
        @NotNull MetodoPago metodoPago,
        @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
        String referencia) { }
