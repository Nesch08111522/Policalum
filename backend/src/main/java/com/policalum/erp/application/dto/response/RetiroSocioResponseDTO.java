package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoRetiro;
import com.policalum.erp.domain.enums.OrigenFondos;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RetiroSocioResponseDTO(
        UUID id,
        UUID adminId,
        String adminNombre,
        UUID sedeId,
        String sedeNombre,
        UUID turnoCajaId,
        BigDecimal montoRetirado,
        OrigenFondos origenFondos,
        String descripcion,
        LocalDateTime fechaRetiro,
        EstadoRetiro estado,
        String motivoAnulacion,
        UUID anuladoPorId,
        String anuladoPorNombre,
        LocalDateTime fechaAnulacion,
        Boolean impactoCajaRevertido,
        LocalDateTime createdAt
) { }
