package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SaldoCuentaResponseDTO(
        UUID id,
        UUID cuentaId,
        BigDecimal saldo,
        LocalDateTime fechaCorte,
        String observacion,
        String registradoPorNombre,
        LocalDateTime createdAt) { }
