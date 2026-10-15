package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoComision;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ComisionResponseDTO(
        UUID id,
        UUID userId,
        String empleadoNombre,
        UUID sedeId,
        String sedeNombre,
        String periodo,
        BigDecimal totalVentasNetas,
        Long cantidadVentas,
        BigDecimal porcentajeComision,
        BigDecimal montoComision,
        EstadoComision estado,
        String aprobadoPorNombre,
        LocalDateTime fechaAprobacion,
        LocalDateTime updatedAt
) { }
