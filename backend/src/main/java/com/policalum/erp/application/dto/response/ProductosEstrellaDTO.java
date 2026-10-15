package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.CriterioRankingProducto;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Top N de productos (sin servicios) según el criterio, tomado del reporte de rentabilidad. */
public record ProductosEstrellaDTO(
        LocalDate desde,
        LocalDate hasta,
        UUID sedeId,
        CriterioRankingProducto criterio,
        int top,
        List<RentabilidadItemDTO> items) { }
