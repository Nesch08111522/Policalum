package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Reporte de mermas: resumen por producto (todo el rango) + detalle paginado.
 * desde/hasta null = todo el historial (incluye mermas antiguas sin fecha).
 */
public record ReporteMermasDTO(
        LocalDate desde,
        LocalDate hasta,
        UUID productoId,
        UUID almacenId,
        long movimientos,
        BigDecimal valorTotal,
        List<MermaPorProductoDTO> porProducto,
        List<MermaKardexDTO> detalle,
        int pagina,
        int tamanoPagina,
        long totalElementos,
        int totalPaginas) { }
