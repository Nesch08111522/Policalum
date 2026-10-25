package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.TipoGastoEmpresarial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record GastoEmpresarialResponseDTO(
        UUID id,
        UUID sedeId,
        String sedeNombre,
        TipoGastoEmpresarial tipo,
        BigDecimal monto,
        LocalDate fecha,
        String descripcion,
        String registradoPor,
        LocalDateTime createdAt
) {}
