package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.model.ConfiguracionReportes;

/** Parámetros de reportes vigentes. También es el snapshot que leen Kardex (M10) y Auditoría (M13). */
public record ParametrosReportesDTO(int maxFilasExportacion) {

    /** Valores de fábrica: idénticos a las constantes anteriores al Módulo 14. */
    public static final ParametrosReportesDTO DEFAULTS =
            new ParametrosReportesDTO(ConfiguracionReportes.MAX_FILAS_EXPORTACION_DEFAULT);
}
