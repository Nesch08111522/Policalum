package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record VentaProcesadaResponseDTO(
        UUID id, String serieCorrelativo, BigDecimal subtotalNeto,
        BigDecimal totalIgv, BigDecimal total, String estado) { }
