package com.policalum.erp.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Las filas que crea el inicializador (builder vacío + @PrePersist) deben tener exactamente los valores
 * que estaban hardcodeados antes del Módulo 14: así el despliegue no cambia el comportamiento.
 */
class ConfiguracionEntidadesTest {

    @Test
    void filasNuevas_tienenLosValoresDeFabrica() {
        ConfiguracionVentas v = ConfiguracionVentas.builder().build();
        v.prePersist();
        assertEquals(1L, v.getId());
        assertEquals(15, v.getVigenciaCotizacionDias());
        assertEquals(120, v.getPreformaMinutosDefault());
        assertEquals(5, v.getPreformaMinutosMin());
        assertEquals(4320, v.getPreformaMinutosMax());
        assertNotNull(v.getUpdatedAt());

        ConfiguracionInventario i = ConfiguracionInventario.builder().build();
        i.prePersist();
        assertEquals(new BigDecimal("500.00"), i.getUmbralJustificacionAjuste());
        assertEquals(30, i.getMinCaracteresJustificacion());

        ConfiguracionFinanzas f = ConfiguracionFinanzas.builder().build();
        f.prePersist();
        assertEquals(3, f.getMesesHistoricosProyeccion());
        assertEquals(3, f.getMesesProyeccionDefault());
        assertEquals(12, f.getMesesProyeccionMax());

        ConfiguracionReportes r = ConfiguracionReportes.builder().build();
        r.prePersist();
        assertEquals(50_000, r.getMaxFilasExportacion());

        ConfiguracionEmpresa e = ConfiguracionEmpresa.builder().build();
        e.prePersist();
        assertEquals(ConfiguracionEmpresa.NOMBRE_COMERCIAL_DEFAULT, e.getNombreComercial());
        assertNull(e.getRuc());
    }

    @Test
    void prePersist_noPisaValoresYaAsignados() {
        ConfiguracionVentas v = ConfiguracionVentas.builder().id(1L).vigenciaCotizacionDias(30).build();
        v.prePersist();
        assertEquals(30, v.getVigenciaCotizacionDias());
        assertEquals(120, v.getPreformaMinutosDefault());

        SerieComprobante s = SerieComprobante.builder().activa(false).build();
        s.prePersist();
        assertFalse(s.getActiva());
        assertNotNull(s.getCreatedAt());
        SerieComprobante nueva = SerieComprobante.builder().build();
        nueva.prePersist();
        assertTrue(nueva.getActiva());
    }
}
