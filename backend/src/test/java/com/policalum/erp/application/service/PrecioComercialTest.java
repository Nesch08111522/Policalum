package com.policalum.erp.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.policalum.erp.domain.model.Producto;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PrecioComercialTest {

    @Test
    void laVentaUsaElPrecioDeOfertaYLaGananciaSaleDelCosto() {
        LocalDate hoy = LocalDate.now(PrecioComercial.LIMA);
        Producto producto = Producto.builder()
                .precioCompra(new BigDecimal("10.00"))
                .precioVentaBase(new BigDecimal("20.00"))
                .precioOferta(new BigDecimal("15.00"))
                .ofertaDesde(hoy)
                .ofertaHasta(hoy)
                .build();

        assertTrue(PrecioComercial.vigente(producto));
        assertEquals(new BigDecimal("15.00"), PrecioComercial.venta(producto));
        assertEquals(new BigDecimal("5.00"), producto.getPrecioOferta().subtract(producto.getPrecioCompra()));
        assertEquals(new BigDecimal("7.50"), PrecioComercial.fraccion(producto, new BigDecimal("10.00")));
    }

    @Test
    void fueraDeFechaSeCobraElPrecioDeLista() {
        LocalDate hoy = LocalDate.now(PrecioComercial.LIMA);
        Producto producto = Producto.builder()
                .precioVentaBase(new BigDecimal("20.00"))
                .precioOferta(new BigDecimal("15.00"))
                .ofertaDesde(hoy.minusDays(10))
                .ofertaHasta(hoy.minusDays(1))
                .oferta(true)
                .build();

        assertFalse(PrecioComercial.vigente(producto));
        assertEquals(new BigDecimal("20.00"), PrecioComercial.venta(producto));
        assertFalse(PrecioComercial.mostrar(producto));
    }
}
