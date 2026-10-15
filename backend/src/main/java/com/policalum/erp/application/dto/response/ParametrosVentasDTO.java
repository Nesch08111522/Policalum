package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.model.ConfiguracionVentas;

/** Parámetros de ventas vigentes. También es el snapshot que leen Cotización (M3) y Preforma (M18). */
public record ParametrosVentasDTO(
        int vigenciaCotizacionDias,
        int preformaMinutosDefault,
        int preformaMinutosMin,
        int preformaMinutosMax) {

    /** Valores de fábrica: idénticos a las constantes anteriores al Módulo 14. */
    public static final ParametrosVentasDTO DEFAULTS = new ParametrosVentasDTO(
            ConfiguracionVentas.VIGENCIA_COTIZACION_DIAS_DEFAULT, ConfiguracionVentas.PREFORMA_MINUTOS_DEFAULT,
            ConfiguracionVentas.PREFORMA_MINUTOS_MIN_DEFAULT, ConfiguracionVentas.PREFORMA_MINUTOS_MAX_DEFAULT);
}
