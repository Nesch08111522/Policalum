package com.policalum.erp.application.dto.request;

import jakarta.validation.Valid;

/**
 * Edición de parámetros (Módulo 14). Cada dominio es opcional: null = no se modifica; uno enviado
 * reemplaza todos sus valores. Debe venir al menos un dominio.
 */
public record ParametrosRequestDTO(
        @Valid ParametrosVentasRequestDTO ventas,
        @Valid ParametrosInventarioRequestDTO inventario,
        @Valid ParametrosFinanzasRequestDTO finanzas,
        @Valid ParametrosReportesRequestDTO reportes) { }
