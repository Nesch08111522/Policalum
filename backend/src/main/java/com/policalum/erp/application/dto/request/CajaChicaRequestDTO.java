package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.ConceptoCajaChica;
import com.policalum.erp.domain.enums.TipoCajaChica;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CajaChicaRequestDTO(
        @NotNull TipoCajaChica tipo,
        @NotNull ConceptoCajaChica concepto,
        @NotNull @DecimalMin("0.01") BigDecimal monto,
        @Size(max = 255) String detalle
) {}
