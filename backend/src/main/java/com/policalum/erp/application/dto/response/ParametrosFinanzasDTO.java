package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.model.ConfiguracionFinanzas;

/** Parámetros de finanzas vigentes. También es el snapshot que lee FlujoCaja (M6). */
public record ParametrosFinanzasDTO(
        int mesesHistoricosProyeccion,
        int mesesProyeccionDefault,
        int mesesProyeccionMax) {

    /** Valores de fábrica: idénticos a las constantes anteriores al Módulo 14. */
    public static final ParametrosFinanzasDTO DEFAULTS = new ParametrosFinanzasDTO(
            ConfiguracionFinanzas.MESES_HISTORICOS_DEFAULT, ConfiguracionFinanzas.MESES_PROYECCION_DEFAULT,
            ConfiguracionFinanzas.MESES_PROYECCION_MAX_DEFAULT);
}
