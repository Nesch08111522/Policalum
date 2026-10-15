package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Reglas de ajustes de stock: sobre el umbral (S/) se exige una justificación de minCaracteres. */
public record ParametrosInventarioRequestDTO(
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal umbralJustificacionAjuste,
        @NotNull @Min(1) @Max(500) Integer minCaracteresJustificacion) { }
