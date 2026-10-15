package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.CriterioRankingProveedor;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Ranking de proveedores por fecha de emisión de la compra (Módulo 12). */
public record ProveedoresRankingDTO(
        LocalDate desde,
        LocalDate hasta,
        UUID sedeId,
        CriterioRankingProveedor criterio,
        int top,
        long comprasOtraMoneda,
        boolean montosVisibles,
        List<ProveedorRankingDTO> items,
        List<String> advertencias) { }
