package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.model.PagoVenta;
import com.policalum.erp.domain.model.Venta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PagoVentaResponseDTO(
        UUID id,
        Venta venta,
        MetodoPago metodoPago,
        BigDecimal monto,
        String referencia
) {
}
