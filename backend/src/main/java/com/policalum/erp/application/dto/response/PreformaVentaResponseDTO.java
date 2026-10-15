package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoPreforma;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** minutosRestantes: solo para ABIERTA (0 en los demás estados). */
public record PreformaVentaResponseDTO(
        UUID id,
        String codigo,
        UUID sedeId,
        String sedeNombre,
        UUID vendedorId,
        String vendedorNombre,
        UUID clienteId,
        String clienteNombre,
        EstadoPreforma estado,
        BigDecimal subtotalNetoEstimado,
        BigDecimal totalIgvEstimado,
        BigDecimal totalEstimado,
        LocalDateTime fechaExpiracion,
        long minutosRestantes,
        String observaciones,
        UUID ventaId,
        String ventaSerie,
        String confirmadoPorNombre,
        LocalDateTime fechaConfirmacion,
        String canceladoPorNombre,
        LocalDateTime fechaCancelacion,
        String motivoCancelacion,
        LocalDateTime createdAt,
        List<DetallePreformaResponseDTO> lineas
) { }
