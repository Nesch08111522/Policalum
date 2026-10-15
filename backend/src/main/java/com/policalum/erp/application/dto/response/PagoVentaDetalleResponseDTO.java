package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.MetodoPago;
import java.math.BigDecimal;
import java.util.UUID;

public record PagoVentaDetalleResponseDTO(UUID id, MetodoPago metodoPago, BigDecimal monto, String referencia) { }
