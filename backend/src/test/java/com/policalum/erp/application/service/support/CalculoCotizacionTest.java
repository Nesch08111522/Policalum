package com.policalum.erp.application.service.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculoCotizacionTest {

    @Test
    void movilidadDentroDelBaseNoSumaExtra() {
        assertEquals(new BigDecimal("20.00"),
                CalculoCotizacion.movilidad(new BigDecimal("20"), 10, new BigDecimal("2"), new BigDecimal("8")));
    }

    @Test
    void movilidadEnElLimiteSigueSiendoElBase() {
        assertEquals(new BigDecimal("20.00"),
                CalculoCotizacion.movilidad(new BigDecimal("20"), 10, new BigDecimal("2"), new BigDecimal("10")));
    }

    @Test
    void movilidadCobraLasPiezasQuePasanDelBase() {
        assertEquals(new BigDecimal("24.00"),
                CalculoCotizacion.movilidad(new BigDecimal("20"), 10, new BigDecimal("2"), new BigDecimal("12")));
    }

    @Test
    void porcentajeRedondeaADosDecimales() {
        assertEquals(new BigDecimal("18.00"),
                CalculoCotizacion.porcentaje(new BigDecimal("180.00"), new BigDecimal("10")));
    }
}
