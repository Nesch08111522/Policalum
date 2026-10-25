package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** fechaCorte opcional: fecha (YYYY-MM-DD) o fecha-hora. Si no se envía, el saldo es a este momento. */
public record RegistrarSaldoCuentaRequestDTO(
        @NotNull @Digits(integer = 10, fraction = 2) BigDecimal saldo,
        @Size(max = 30) String fechaCorte,
        @Size(max = 500) String observacion) { }
