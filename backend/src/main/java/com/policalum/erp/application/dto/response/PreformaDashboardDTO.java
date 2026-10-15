package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * @param tasaConversionPorcentaje confirmadas / (confirmadas + canceladas + expiradas) × 100, por cantidad
 * @param montoPerdido             total estimado de canceladas + expiradas (ventas que no se concretaron)
 * @param abiertasAhora            preformas ABIERTAS vigentes en este momento (sin filtro de fechas)
 */
public record PreformaDashboardDTO(
        UUID sedeId,
        LocalDate desde,
        LocalDate hasta,
        Map<String, Long> preformasPorEstado,
        BigDecimal montoConfirmado,
        BigDecimal montoPerdido,
        BigDecimal tasaConversionPorcentaje,
        long abiertasAhora,
        BigDecimal montoAbiertoAhora
) { }
