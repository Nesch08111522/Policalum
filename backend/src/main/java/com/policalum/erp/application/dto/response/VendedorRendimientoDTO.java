package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;

public record VendedorRendimientoDTO(String vendedor, long ventas, BigDecimal total) {}
