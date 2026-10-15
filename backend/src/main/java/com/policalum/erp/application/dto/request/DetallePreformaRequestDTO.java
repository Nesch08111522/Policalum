package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Exactamente uno de productoId o servicioId. El precio lo calcula el backend. */
public record DetallePreformaRequestDTO(
        UUID productoId,
        UUID fraccionId,
        UUID servicioId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
        @Digits(integer = 8, fraction = 4, message = "Cantidad inválida")
        BigDecimal cantidad
) { }
