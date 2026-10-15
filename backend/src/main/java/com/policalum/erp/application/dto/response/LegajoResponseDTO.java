package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.SistemaPension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LegajoResponseDTO(
        UUID id,
        UUID empleadoId,
        String empleadoNombre,
        String empleadoEmail,
        UUID sedeId,
        String sedeNombre,
        String tipoContrato,
        LocalDate fechaIngreso,
        BigDecimal sueldoBase,
        BigDecimal porcentajeComision,
        SistemaPension sistemaPension,
        BigDecimal tasaPension,
        Boolean activo,
        LocalDateTime createdAt,
        List<LegajoDocumentoResponseDTO> documentos
) { }
