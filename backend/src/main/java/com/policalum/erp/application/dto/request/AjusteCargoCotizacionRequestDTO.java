package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** La empresa corrige utilidad, pasaje y otros cargos, y deja escrito qué cambió. */
public record AjusteCargoCotizacionRequestDTO(
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal utilidadPorcentaje,
        @NotNull @DecimalMin("0.00") BigDecimal movilidadMonto,
        @NotNull @DecimalMin("0.00") BigDecimal otrosMonto,
        @Size(max = 500) String otrosDetalle,
        @NotBlank @Size(max = 500) String notaCambio
) {}
