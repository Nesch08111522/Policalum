package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.EstadoNomina;
import com.policalum.erp.domain.model.RrhhNomina;
import com.policalum.erp.domain.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RrhhNominaRequestDTO(
        User user,
        String periodo,
        BigDecimal sueldoBaseCalculado,
        BigDecimal totalComisiones,
        BigDecimal descuentos,
        BigDecimal totalNeto,
        EstadoNomina estado,
        User aprobadoPor,
        LocalDateTime fechaAprobacion
) {
}
