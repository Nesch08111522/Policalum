package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoDevolucion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DevolucionListadoDTO(
        UUID id,
        String codigo,
        UUID ventaId,
        String ventaSerie,
        String clienteNombre,
        String sedeNombre,
        EstadoDevolucion estado,
        BigDecimal montoTotal,
        LocalDateTime fechaSolicitud
) { }
