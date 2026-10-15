package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoDevolucion;
import com.policalum.erp.domain.enums.EstadoReembolso;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DevolucionResponseDTO(
        UUID id,
        String codigo,
        UUID ventaId,
        String ventaSerie,
        UUID sedeId,
        String sedeNombre,
        UUID clienteId,
        String clienteNombre,
        String motivo,
        EstadoDevolucion estado,
        BigDecimal montoTotal,
        String solicitadoPorNombre,
        LocalDateTime fechaSolicitud,
        String resueltoPorNombre,
        LocalDateTime fechaResolucion,
        String observacionAdmin,
        List<DetalleDevolucionResponseDTO> detalles,
        UUID reembolsoId,
        EstadoReembolso reembolsoEstado
) { }
