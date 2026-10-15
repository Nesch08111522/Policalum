package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * cantidad con signo: positiva = sobrante (ENTRADA), negativa = faltante/merma (SALIDA).
 * justificacion es obligatoria (mín. 30 caracteres) si el ajuste vale más de S/ 500.
 */
public record AjusteStockRequestDTO(
        @NotNull UUID productoId,
        @NotNull UUID almacenId,
        @NotNull @Digits(integer = 8, fraction = 4) BigDecimal cantidad,
        @NotBlank @Size(max = 255) String motivo,
        @Size(max = 2000) String justificacion) { }
