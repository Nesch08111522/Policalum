package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoReembolso;
import com.policalum.erp.domain.enums.MetodoReembolso;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReembolsoResponseDTO(
        UUID id,
        UUID devolucionId,
        String devolucionCodigo,
        UUID ventaId,
        String clienteNombre,
        BigDecimal montoMaximo,
        BigDecimal montoTotalReembolso,
        String motivoAjuste,
        MetodoReembolso metodoReembolso,
        String referencia,
        EstadoReembolso estado,
        String creadoPorNombre,
        LocalDateTime createdAt,
        String procesadoPorNombre,
        LocalDateTime fechaProcesado,
        UUID turnoCajaId,
        String observaciones
) { }
