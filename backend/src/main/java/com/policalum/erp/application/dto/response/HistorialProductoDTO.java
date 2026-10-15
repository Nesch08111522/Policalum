package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Historial de un producto: saldos actuales por almacén + movimientos (más recientes primero,
 * paginados) con el stock resultante de cada uno. totalEntradas/totalSalidas son del rango filtrado.
 */
public record HistorialProductoDTO(
        UUID productoId,
        String productoNombre,
        String codigoSku,
        String unidad,
        UUID almacenId,
        LocalDate desde,
        LocalDate hasta,
        BigDecimal stockActual,
        List<SaldoKardexDTO> saldosPorAlmacen,
        BigDecimal totalEntradas,
        BigDecimal totalSalidas,
        List<KardexMovimientoDetalleDTO> movimientos,
        int pagina,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas) { }
