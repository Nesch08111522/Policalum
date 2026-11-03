package com.policalum.erp.application.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Montos del formato de cotización. Los precios de catálogo ya incluyen IGV. */
public final class CalculoCotizacion {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private CalculoCotizacion() {}

    public static BigDecimal porcentaje(BigDecimal base, BigDecimal porcentaje) {
        if (base == null || porcentaje == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return base.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    /**
     * El precio base cubre hasta {@code unidadesIncluidas} piezas.
     * Cada pieza por encima suma {@code precioUnidadExtra}.
     */
    public static BigDecimal movilidad(BigDecimal precioBase, int unidadesIncluidas, BigDecimal precioUnidadExtra, BigDecimal cantidad) {
        BigDecimal base = precioBase == null ? BigDecimal.ZERO : precioBase;
        BigDecimal extra = precioUnidadExtra == null ? BigDecimal.ZERO : precioUnidadExtra;
        BigDecimal piezas = cantidad == null ? BigDecimal.ZERO : cantidad;
        int incluidas = Math.max(unidadesIncluidas, 0);
        BigDecimal sobrante = piezas.subtract(BigDecimal.valueOf(incluidas));
        if (sobrante.signum() < 0) sobrante = BigDecimal.ZERO;
        return base.add(sobrante.multiply(extra)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal dinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
