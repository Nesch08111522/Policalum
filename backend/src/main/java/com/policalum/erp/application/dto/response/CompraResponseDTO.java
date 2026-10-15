package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoCompra;
import com.policalum.erp.domain.enums.EstadoPago;
import com.policalum.erp.domain.enums.EstadoRecepcion;
import com.policalum.erp.domain.enums.Moneda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CompraResponseDTO(
        UUID id,
        String numeroOrdenCompra,
        UUID proveedorId,
        String proveedorNombre,
        String proveedorDocumento,
        UUID sedeId,
        String sedeNombre,
        UUID userId,
        String userNombre,
        String numeroFacturaProveedor,
        LocalDate fechaEmision,
        LocalDate fechaVencimientoPago,
        Moneda moneda,
        EstadoCompra estadoCompra,
        EstadoRecepcion estadoRecepcion,
        EstadoPago estadoPago,
        BigDecimal subtotalNeto,
        BigDecimal totalIgv,
        BigDecimal total,
        String observaciones,
        LocalDateTime fechaAnulacion,
        String motivoAnulacion,
        LocalDateTime createdAt,
        List<DetalleCompraResponseDTO> detalles,
        List<CompraAdjuntoResponseDTO> adjuntos
) { }