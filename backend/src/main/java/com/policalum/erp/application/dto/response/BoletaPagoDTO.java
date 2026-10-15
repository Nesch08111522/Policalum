package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoNomina;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Datos de la boleta de pago para que el frontend la renderice o imprima. */
public record BoletaPagoDTO(
        UUID nominaId,
        String periodo,
        UUID empleadoId,
        String empleadoNombre,
        String empleadoEmail,
        String sedeNombre,
        String tipoContrato,
        LocalDate fechaIngreso,
        Integer diasComputados,
        List<BoletaConceptoDTO> ingresos,
        BigDecimal totalIngresos,
        List<BoletaConceptoDTO> descuentos,
        BigDecimal totalDescuentos,
        BigDecimal netoAPagar,
        EstadoNomina estado,
        LocalDateTime fechaPago
) { }
