package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @param programadosPorEstado         órdenes programadas para la fecha, por estado
 * @param atrasados                    PENDIENTE/EN_CAMINO con fecha programada anterior a la fecha
 * @param tasaEntregaPrimerIntento     % de las entregas del día logradas sin fallos previos
 */
public record DespachoDashboardDTO(
        UUID sedeId,
        LocalDate fecha,
        Map<String, Long> programadosPorEstado,
        long atrasados,
        long entregadosEnLaFecha,
        BigDecimal tasaEntregaPrimerIntento,
        List<CargaRepartidorDTO> cargaPorRepartidor
) {
    public record CargaRepartidorDTO(UUID responsableId, String nombre, long pendientes, long enCamino) { }
}
