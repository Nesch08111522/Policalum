package com.policalum.erp.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Valores por defecto de @PrePersist (fixes de los Módulos 8). Mismo paquete para invocar prePersist(). */
class PrePersistEntidadesTest {

    // ===================== RrhhAsistencia =====================

    @Test
    void asistencia_marcacionManualDeDiaPasado_conservaSuFecha() {
        RrhhAsistencia a = new RrhhAsistencia();
        LocalDate ayer = LocalDate.now().minusDays(1);
        a.setFechaHora(ayer.atTime(8, 5));
        a.setFecha(ayer);

        a.prePersist();

        assertEquals(ayer, a.getFecha());
        assertNotNull(a.getCreatedAt());
    }

    @Test
    void asistencia_sinFecha_usaHoy() {
        RrhhAsistencia a = new RrhhAsistencia();

        a.prePersist();

        assertEquals(LocalDate.now(), a.getFecha());
    }

    // ===================== RrhhTurnoTrabajo =====================

    @Test
    void turno_creadoConBuilderSinValores_tomaDefaults() {
        // @Builder ignora los inicializadores: activo y createdAt llegan null
        RrhhTurnoTrabajo t = RrhhTurnoTrabajo.builder().build();
        assertNull(t.getActivo());
        assertNull(t.getToleranciaMinutos());

        t.prePersist();

        assertTrue(t.getActivo());
        assertEquals(RrhhTurnoTrabajo.TOLERANCIA_DEFAULT_MINUTOS, t.getToleranciaMinutos());
        assertNotNull(t.getCreatedAt());
    }

    @Test
    void turno_conToleranciaExplicita_noSeSobrescribe() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 1, 9, 0);
        RrhhTurnoTrabajo t = RrhhTurnoTrabajo.builder().toleranciaMinutos(0).activo(false).createdAt(creado).build();

        t.prePersist();

        assertEquals(0, t.getToleranciaMinutos());
        assertFalse(t.getActivo());
        assertEquals(creado, t.getCreatedAt());
    }

    @Test
    void turno_toleranciaSegura_conColumnaNulaDeTurnosAntiguos() {
        RrhhTurnoTrabajo antiguo = new RrhhTurnoTrabajo();
        antiguo.setToleranciaMinutos(null); // filas creadas antes del Módulo 8

        assertEquals(10, antiguo.getToleranciaSegura());
    }
}
