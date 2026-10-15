package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Uso de un vehículo en el rango (por fecha programada del despacho).
 * tasaExito = entregados / (entregados + fallidos) × 100; null si no hubo entregas cerradas.
 */
public record UsoMovilidadDTO(
        UUID movilidadId,
        String placa,
        String tipo,
        String conductorNombre,
        boolean activo,
        long despachos,
        long entregados,
        long fallidos,
        long pendientes,
        long intentosFallidos,
        BigDecimal tasaExitoPorcentaje) { }
