package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Proveedor en el ranking (Módulo 12). montoRecibido = compras RECIBIDAS en PEN (con IGV).
 * diasEntrega = de la emisión de la orden a su entrada en Kardex (no hay fecha de entrega prometida).
 * Montos null para MULTIFUNCIONAL.
 */
public record ProveedorRankingDTO(
        int posicion,
        UUID proveedorId,
        String nombre,
        String numeroDocumento,
        long compras,
        long recibidas,
        long pendientes,
        long anuladas,
        BigDecimal montoRecibido,
        BigDecimal ticketPromedio,
        LocalDate ultimaCompra,
        BigDecimal diasEntregaPromedio,
        Integer diasEntregaMaximo) {

    /** Copia sin montos (vista MULTIFUNCIONAL). */
    public ProveedorRankingDTO sinMontos() {
        return new ProveedorRankingDTO(posicion, proveedorId, nombre, numeroDocumento, compras, recibidas, pendientes,
                anuladas, null, null, ultimaCompra, diasEntregaPromedio, diasEntregaMaximo);
    }
}
