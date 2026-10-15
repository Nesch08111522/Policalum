package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.MetodoPago;
import java.math.BigDecimal;

/** Cobros por método de pago; porcentaje sobre el total cobrado. */
public record MetodoPagoMontoDTO(MetodoPago metodo, long pagos, BigDecimal total, BigDecimal porcentaje) { }
