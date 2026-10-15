package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.response.UtilidadNetaResponseDTO;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Utilidad neta real (versión mínima del Módulo 5; el Módulo 6 la amplía con flujo de caja).
 * Utilidad Neta = (Ventas - Compras recibidas) - Nóminas pagadas. Retiros de socios: informativo.
 */
public interface UtilidadNetaService {

    /**
     * Calcula la utilidad en un rango de fechas (inclusivo). Por defecto, mes en curso.
     * Las nóminas se toman por mes completo (su periodo es "YYYY-MM").
     */
    UtilidadNetaResponseDTO calcular(UUID sedeId, LocalDate desde, LocalDate hasta);

    /**
     * Calcula la utilidad de un mes.
     *
     * @param periodo formato "YYYY-MM", p. ej. "2026-10"
     * @throws IllegalArgumentException si el formato es inválido (400)
     */
    UtilidadNetaResponseDTO calcularPorPeriodo(UUID sedeId, String periodo);
}
