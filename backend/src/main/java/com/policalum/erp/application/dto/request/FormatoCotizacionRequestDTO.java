package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.TipoCargoCotizacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record FormatoCotizacionRequestDTO(
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal utilidadPorcentaje,
        @NotEmpty @Size(max = 30) List<@Valid Distrito> distritos,
        @Size(max = 12) List<@Valid Concepto> conceptos
) {
    public record Distrito(
            @NotBlank @Size(max = 80) String distrito,
            @NotNull @DecimalMin("0.00") BigDecimal precioBase,
            @NotNull @Min(0) Integer unidadesIncluidas,
            @NotNull @DecimalMin("0.00") BigDecimal precioUnidadExtra,
            @NotNull Boolean activo
    ) {}

    public record Concepto(
            @NotBlank @Size(max = 80) String nombre,
            @NotNull TipoCargoCotizacion tipo,
            @NotNull @DecimalMin("0.00") BigDecimal valor,
            @NotNull Boolean activo
    ) {}
}
