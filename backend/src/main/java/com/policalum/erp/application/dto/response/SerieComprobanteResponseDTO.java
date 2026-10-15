package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.TipoComprobante;
import java.time.LocalDateTime;
import java.util.UUID;

public record SerieComprobanteResponseDTO(
        UUID id,
        UUID sedeId,
        String sedeNombre,
        TipoComprobante tipoComprobante,
        String serie,
        boolean activa,
        LocalDateTime updatedAt) { }
