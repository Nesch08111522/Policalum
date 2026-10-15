package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.domain.enums.TipoComprobante;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Filtros del reporte de ventas (Módulo 12). Todos opcionales: sin fechas = mes en curso; estado null =
 * COMPLETADA. Se arma en el controller desde query params (no lleva validaciones de bean: el servicio
 * valida el rango y los permisos).
 */
public record FiltroVentasReporteDTO(
        LocalDate desde,
        LocalDate hasta,
        UUID sedeId,
        UUID vendedorId,
        UUID clienteId,
        CanalVenta canal,
        TipoComprobante tipoComprobante,
        EstadoVenta estado) {

    /** true si hay filtros que las devoluciones no pueden replicar (vendedor, cliente, canal, tipo). */
    public boolean filtraMasQueSede() {
        return vendedorId != null || clienteId != null || canal != null || tipoComprobante != null;
    }
}
