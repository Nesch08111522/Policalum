package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record DetalleCotizacionResponseDTO(UUID id, UUID productoId, String productoNombre,
        UUID fraccionId, String fraccionNombre, UUID servicioId, String servicioNombre,
        BigDecimal cantidad, BigDecimal precioUnitarioConIgv, BigDecimal valorUnitarioNeto,
        BigDecimal igvLinea, BigDecimal porcentajeIgv, BigDecimal subtotal) { }
