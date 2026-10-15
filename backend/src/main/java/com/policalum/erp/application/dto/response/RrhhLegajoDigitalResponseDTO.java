package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.model.RrhhLegajoDigital;
import com.policalum.erp.domain.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RrhhLegajoDigitalResponseDTO(
        UUID id,
        User user,
        String tipoContrato,
        LocalDate fechaIngreso,
        BigDecimal sueldoBase,
        BigDecimal porcentajeComision,
        Map<String, Object> documentosJson,
        Boolean activo
) {
}
