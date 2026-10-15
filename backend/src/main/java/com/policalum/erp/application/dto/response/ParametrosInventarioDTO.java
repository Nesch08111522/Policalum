package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.model.ConfiguracionInventario;
import java.math.BigDecimal;

/** Parámetros de inventario vigentes. También es el snapshot que lee StockOperacion (M11). */
public record ParametrosInventarioDTO(
        BigDecimal umbralJustificacionAjuste,
        int minCaracteresJustificacion) {

    /** Valores de fábrica: idénticos a las constantes anteriores al Módulo 14. */
    public static final ParametrosInventarioDTO DEFAULTS = new ParametrosInventarioDTO(
            ConfiguracionInventario.UMBRAL_JUSTIFICACION_DEFAULT, ConfiguracionInventario.MIN_CARACTERES_JUSTIFICACION_DEFAULT);
}
