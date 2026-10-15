package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogoFraccionResponseDTO(
        UUID id,
        String nombreFraccion,
        BigDecimal factorDescuentoKardex,
        BigDecimal precioVenta,
        Boolean activo
) {
}