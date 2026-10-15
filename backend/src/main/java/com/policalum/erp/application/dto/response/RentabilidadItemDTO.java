package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Rentabilidad de un producto o servicio (Módulo 12). Cantidades en unidades base (planchas completas
 * equivalentes); montos netos sin IGV. costoUnitario = costo promedio ponderado ACTUAL del producto
 * (las ventas no guardan el costo histórico). Servicios: sin costo. Campos de costo null para MULTIFUNCIONAL.
 */
public record RentabilidadItemDTO(
        String tipo,
        UUID id,
        String nombre,
        String sku,
        String categoria,
        String unidad,
        long ventas,
        BigDecimal cantidadVendida,
        BigDecimal cantidadDevuelta,
        BigDecimal cantidadNeta,
        BigDecimal ingresoNeto,
        BigDecimal devolucionesNeto,
        BigDecimal ingresoNetoReal,
        BigDecimal participacionPorcentaje,
        BigDecimal costoUnitario,
        BigDecimal costoEstimado,
        BigDecimal margen,
        BigDecimal margenPorcentaje) {

    public static final String PRODUCTO = "PRODUCTO";
    public static final String SERVICIO = "SERVICIO";

    /** Copia sin costo ni margen (vista MULTIFUNCIONAL). */
    public RentabilidadItemDTO sinCostos() {
        return new RentabilidadItemDTO(tipo, id, nombre, sku, categoria, unidad, ventas, cantidadVendida, cantidadDevuelta,
                cantidadNeta, ingresoNeto, devolucionesNeto, ingresoNetoReal, participacionPorcentaje, null, null, null, null);
    }
}
