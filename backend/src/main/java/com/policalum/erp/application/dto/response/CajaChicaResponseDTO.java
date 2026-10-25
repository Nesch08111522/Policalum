package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.ConceptoCajaChica;
import com.policalum.erp.domain.enums.TipoCajaChica;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CajaChicaResponseDTO(
        UUID id,
        UUID turnoId,
        TipoCajaChica tipo,
        ConceptoCajaChica concepto,
        BigDecimal monto,
        String detalle,
        LocalDateTime createdAt
) {}
