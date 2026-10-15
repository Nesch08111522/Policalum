package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RetiroSocioResumenDTO(UUID socioId, String socioNombre, long cantidad, BigDecimal total) { }
