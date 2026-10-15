package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoNomina;
import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.SistemaPension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record NominaResponseDTO(
        UUID id,
        UUID userId,
        String empleadoNombre,
        UUID sedeId,
        String sedeNombre,
        String periodo,
        String tipoContrato,
        Integer diasComputados,
        BigDecimal sueldoBaseCalculado,
        UUID comisionId,
        BigDecimal ventasBaseComision,
        BigDecimal porcentajeComision,
        BigDecimal totalComisiones,
        SistemaPension sistemaPension,
        BigDecimal tasaPension,
        BigDecimal descuentoPension,
        BigDecimal otrosDescuentos,
        BigDecimal descuentos,
        BigDecimal totalNeto,
        EstadoNomina estado,
        String generadoPorNombre,
        String aprobadoPorNombre,
        LocalDateTime fechaAprobacion,
        String pagadoPorNombre,
        LocalDateTime fechaPago,
        MetodoPago metodoPago,
        String referenciaPago,
        String observaciones,
        LocalDateTime createdAt
) { }
