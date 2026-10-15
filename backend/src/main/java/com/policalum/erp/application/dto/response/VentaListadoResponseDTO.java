package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VentaListadoResponseDTO(UUID id, String serieCorrelativo, EstadoVenta estado,
        CanalVenta canalVenta, TipoComprobante tipoComprobante, BigDecimal total,
        UUID clienteId, String clienteNombre, UUID usuarioId, LocalDateTime createdAt) { }
