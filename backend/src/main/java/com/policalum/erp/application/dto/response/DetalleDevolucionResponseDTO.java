package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoProducto;
import java.math.BigDecimal;
import java.util.UUID;

public record DetalleDevolucionResponseDTO(
        UUID id,
        UUID detalleVentaId,
        UUID productoId,
        String productoNombre,
        String fraccionNombre,
        BigDecimal cantidadDevuelta,
        BigDecimal cantidadStock,
        BigDecimal precioUnitarioRef,
        BigDecimal subtotal,
        EstadoProducto estadoProducto,
        String almacenNombre,
        String observacion
) { }
