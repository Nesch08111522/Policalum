package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.domain.enums.TipoComprobante;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Fila del detalle de ventas. metodosPago: "EFECTIVO 50.00 + YAPE 30.00". */
public record VentaReporteDTO(
        UUID id,
        String serieCorrelativo,
        LocalDateTime fecha,
        String sede,
        String vendedor,
        String cliente,
        String clienteDocumento,
        CanalVenta canal,
        TipoComprobante tipoComprobante,
        EstadoVenta estado,
        BigDecimal neto,
        BigDecimal igv,
        BigDecimal total,
        String metodosPago) { }
