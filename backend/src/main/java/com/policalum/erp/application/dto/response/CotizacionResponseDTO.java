package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.CanalCotizacion;
import com.policalum.erp.domain.enums.EstadoCotizacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CotizacionResponseDTO(UUID id, UUID sedeId, String sedeNombre, UUID usuarioId,
        String usuarioNombre, UUID clienteId, String clienteNombre, EstadoCotizacion estado,
        BigDecimal total, LocalDate fechaVigencia, String observaciones, UUID ventaId,
        LocalDateTime createdAt, List<DetalleCotizacionResponseDTO> lineas,
        CanalCotizacion canal, String origen) { }
