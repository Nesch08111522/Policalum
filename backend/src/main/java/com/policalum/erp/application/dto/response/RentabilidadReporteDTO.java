package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Rentabilidad por producto/servicio (Módulo 12), ordenada por ingreso neto real. costoEstimado y margen
 * solo suman productos con costo registrado; margenPorcentaje es sobre el ingreso de esos productos.
 * costosVisibles = false para MULTIFUNCIONAL (todos los campos de costo en null).
 */
public record RentabilidadReporteDTO(
        LocalDate desde,
        LocalDate hasta,
        UUID sedeId,
        UUID categoriaId,
        BigDecimal ingresoNetoReal,
        BigDecimal costoEstimado,
        BigDecimal margen,
        BigDecimal margenPorcentaje,
        boolean costosVisibles,
        List<RentabilidadItemDTO> items,
        List<String> advertencias) { }
