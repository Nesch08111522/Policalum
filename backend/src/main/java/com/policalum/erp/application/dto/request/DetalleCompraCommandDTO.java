package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Línea de compra. Puede ser un producto completo o una fracción del mismo.
 * El almacén destino puede variar por línea.
 */
public record DetalleCompraCommandDTO(
        @NotNull UUID productoId,
        UUID fraccionId,     // opcional: si se compra una fracción específica
        @NotNull UUID almacenId,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal cantidad,
        @NotNull @DecimalMin(value = "0.01") BigDecimal precioUnitarioConIgv
) { }