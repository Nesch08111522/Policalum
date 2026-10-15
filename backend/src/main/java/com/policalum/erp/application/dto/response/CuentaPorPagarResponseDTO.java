package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoCxP;
import com.policalum.erp.domain.enums.TipoReferenciaCxP;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CuentaPorPagarResponseDTO(
        UUID id,
        UUID sedeId,
        String sedeNombre,
        UUID proveedorId,
        String proveedorNombre,
        String proveedorDocumento,
        UUID compraId,
        String compraNumeroOC,
        TipoReferenciaCxP tipoReferencia,
        String descripcion,
        BigDecimal montoTotal,
        BigDecimal montoPagado,
        BigDecimal saldoPendiente,
        EstadoCxP estado,
        LocalDate fechaVencimiento,
        Boolean vencida,
        LocalDateTime fechaPago,
        String metodoPago,
        UUID pagadoPorId,
        String pagadoPorNombre,
        LocalDateTime createdAt
) { }