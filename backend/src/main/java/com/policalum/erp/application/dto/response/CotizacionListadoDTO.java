package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoCotizacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
public record CotizacionListadoDTO(UUID id, UUID sedeId, UUID clienteId, String clienteNombre,
        EstadoCotizacion estado, BigDecimal total, LocalDate fechaVigencia, LocalDateTime createdAt) { }
