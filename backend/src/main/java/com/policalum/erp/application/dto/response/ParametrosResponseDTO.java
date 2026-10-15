package com.policalum.erp.application.dto.response;

/** Todos los parámetros de negocio editables, agrupados por dominio. */
public record ParametrosResponseDTO(
        ParametrosVentasDTO ventas,
        ParametrosInventarioDTO inventario,
        ParametrosFinanzasDTO finanzas,
        ParametrosReportesDTO reportes) { }
