package com.policalum.erp.infrastructure.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PeruGeocoderTest {

    @Test
    void buscar_direccionConCiudad_marcaLaCiudad() {
        double[] punto = PeruGeocoder.buscar("Lima", "Av. Principal 123", "Sede Principal");
        assertNotNull(punto);
        assertEquals(-12.0464, punto[0], 0.0001);
    }

    @Test
    void buscar_distritoMasLargo_ganaSobreLaCiudadCorta() {
        double[] punto = PeruGeocoder.buscar("San Juan de Miraflores", null, null);
        assertNotNull(punto);
        assertEquals(-12.16, punto[0], 0.001);
    }

    @Test
    void buscar_textoSinCiudad_noInventaUnPunto() {
        assertNull(PeruGeocoder.buscar("Av. Los Pinos 50", null, "Almacén"));
    }

    @Test
    void buscar_dosSedesEnLaLibertad_quedanEnDistritosDistintos() {
        double[] huanchaco = PeruGeocoder.buscar("Huanchaco", "La Libertad", "Sede costa");
        double[] larco = PeruGeocoder.buscar("Trujillo", "Víctor Larco Herrera", "Sede centro");
        assertNotNull(huanchaco);
        assertNotNull(larco);
        assertEquals(-8.0814, huanchaco[0], 0.0001);
        assertEquals(-8.1292, larco[0], 0.0001);
        assertEquals(-79.0472, larco[1], 0.0001);
    }
}
