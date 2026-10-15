package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.model.CatalogoFraccion;
import com.policalum.erp.domain.model.Cotizacion;
import com.policalum.erp.domain.model.DetalleCotizacion;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.domain.model.Servicio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DetalleCotizacionRequestDTO(
        Cotizacion cotizacion,
        Producto producto,
        CatalogoFraccion fraccion,
        Servicio servicio,
        BigDecimal cantidad,
        BigDecimal precioUnitarioConIgv,
        BigDecimal valorUnitarioNeto,
        BigDecimal igvLinea,
        BigDecimal porcentajeIgv,
        BigDecimal subtotal
) {
}
