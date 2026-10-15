package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferenciaStockRequestDTO(
        @NotNull UUID productoId,
        @NotNull UUID almacenOrigenId,
        @NotNull UUID almacenDestinoId,
        @NotNull @DecimalMin("0.0001") @Digits(integer = 8, fraction = 4) BigDecimal cantidad,
        @Size(max = 255) String motivo,
        @Size(max = 100) String referenciaDocumento) { }
