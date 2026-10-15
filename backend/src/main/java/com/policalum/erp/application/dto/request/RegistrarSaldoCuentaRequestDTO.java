package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** fechaCorte opcional: si no se envía, el saldo es "a este momento". */
public record RegistrarSaldoCuentaRequestDTO(
        @NotNull @Digits(integer = 10, fraction = 2) BigDecimal saldo,
        @PastOrPresent LocalDateTime fechaCorte,
        @Size(max = 500) String observacion) { }
