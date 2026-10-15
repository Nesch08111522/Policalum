package com.policalum.erp.application.dto.request;

import java.math.BigDecimal;

import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.Compra;
import com.policalum.erp.domain.model.Producto;

public record DetalleCompraRequestDTO(
        Compra compra,
        Producto producto,
        Almacen almacen,
        BigDecimal precioUnitarioConIgv,
        BigDecimal valorUnitarioNeto,
        BigDecimal igvLinea,
        BigDecimal porcentajeIgv,
        BigDecimal subtotal
) {
}
