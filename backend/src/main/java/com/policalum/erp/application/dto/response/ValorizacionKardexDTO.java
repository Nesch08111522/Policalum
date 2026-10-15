package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Inventario valorizado a costo promedio, de mayor a menor valor. almacenId/sedeId indican
 * el filtro aplicado (null = toda la empresa). productosSinCosto: con stock pero costo 0.
 */
public record ValorizacionKardexDTO(
        UUID almacenId,
        String almacenNombre,
        UUID sedeId,
        LocalDateTime calculadoEn,
        BigDecimal valorTotal,
        int productos,
        long productosSinCosto,
        List<ValorizacionProductoDTO> detalle) { }
