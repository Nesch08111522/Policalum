package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoCompra;
import com.policalum.erp.domain.enums.EstadoPago;
import com.policalum.erp.domain.enums.Moneda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CompraListadoDTO(
        UUID id,
        String numeroOrdenCompra,
        String numeroFacturaProveedor,
        String proveedorNombre,
        LocalDate fechaEmision,
        Moneda moneda,
        BigDecimal total,
        EstadoCompra estadoCompra,
        EstadoPago estadoPago,
        LocalDateTime createdAt
) { }