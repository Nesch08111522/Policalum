package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.TipoGastoEmpresarial;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GastoEmpresarialRequestDTO(
        UUID sedeId,
        @NotNull TipoGastoEmpresarial tipo,
        @NotNull @DecimalMin("0.01") BigDecimal monto,
        @NotNull LocalDate fecha,
        @Size(max = 255) String descripcion
) {}
