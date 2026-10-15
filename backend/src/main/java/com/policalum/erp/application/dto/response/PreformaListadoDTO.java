package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoPreforma;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PreformaListadoDTO(
        UUID id,
        String codigo,
        String clienteNombre,
        String vendedorNombre,
        EstadoPreforma estado,
        BigDecimal totalEstimado,
        LocalDateTime fechaExpiracion,
        LocalDateTime createdAt
) { }
