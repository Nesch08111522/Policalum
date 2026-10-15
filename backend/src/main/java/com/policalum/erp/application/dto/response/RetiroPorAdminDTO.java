package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RetiroPorAdminDTO(
        UUID adminId,
        String adminNombre,
        long cantidad,
        BigDecimal total
) { }
