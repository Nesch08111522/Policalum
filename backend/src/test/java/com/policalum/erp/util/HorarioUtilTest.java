package com.policalum.erp.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Módulo 8: días y cruce de turnos, incluidos turnos nocturnos y el salto domingo → lunes. */
class HorarioUtilTest {

    private static LocalTime t(int h, int m) {
        return LocalTime.of(h, m);
    }

    @Test
    void duracionMinutos_normalNocturnoYDiaCompleto() {
        assertEquals(480, HorarioUtil.duracionMinutos(t(8, 0), t(16, 0)));
        assertEquals(480, HorarioUtil.duracionMinutos(t(22, 0), t(6, 0)), "cruza medianoche");
        assertEquals(1440, HorarioUtil.duracionMinutos(t(8, 0), t(8, 0)), "inicio = fin se toma como 24 h");
    }

    @Test
    void aDayOfWeek_aceptaTildesMinusculasYEspacios() {
        assertEquals(DayOfWeek.WEDNESDAY, HorarioUtil.aDayOfWeek(" miércoles "));
        assertEquals(DayOfWeek.SATURDAY, HorarioUtil.aDayOfWeek("Sábado"));
        assertEquals(DayOfWeek.SUNDAY, HorarioUtil.aDayOfWeek("DOMINGO"));
    }

    @Test
    void aDayOfWeek_invalidoONulo_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> HorarioUtil.aDayOfWeek("MONDAY"));
        assertThrows(IllegalArgumentException.class, () -> HorarioUtil.aDayOfWeek(null));
        assertNull(HorarioUtil.aDayOfWeekONull("feriado"));
    }

    @Test
    void incluyeDia_ignoraInvalidosYNulos() {
        assertTrue(HorarioUtil.incluyeDia(List.of("x", "Lunes"), DayOfWeek.MONDAY));
        assertFalse(HorarioUtil.incluyeDia(List.of("Martes"), DayOfWeek.MONDAY));
        assertFalse(HorarioUtil.incluyeDia(null, DayOfWeek.MONDAY));
    }

    @Test
    void normalizarDias_quitaDuplicadosYOrdenaDeLunesADomingo() {
        assertEquals(List.of("LUNES", "MIERCOLES", "DOMINGO"),
                HorarioUtil.normalizarDias(List.of("domingo", "Miércoles", "LUNES", "lunes")));
        assertEquals("VIERNES", HorarioUtil.nombre(DayOfWeek.FRIDAY));
    }

    @Test
    void normalizarDias_vacioOConInvalido_lanza() {
        assertThrows(IllegalArgumentException.class, () -> HorarioUtil.normalizarDias(List.of()));
        assertThrows(IllegalArgumentException.class, () -> HorarioUtil.normalizarDias(null));
        assertThrows(IllegalArgumentException.class, () -> HorarioUtil.normalizarDias(List.of("LUNES", "x")));
    }

    @Test
    void seCruzan_mismoDiaSolapados() {
        assertTrue(HorarioUtil.seCruzan(List.of("LUNES"), t(8, 0), t(14, 0), List.of("LUNES"), t(13, 0), t(18, 0)));
    }

    @Test
    void seCruzan_consecutivosNoSeCruzan() {
        assertFalse(HorarioUtil.seCruzan(List.of("LUNES"), t(8, 0), t(14, 0), List.of("LUNES"), t(14, 0), t(20, 0)),
                "el fin es excluyente");
    }

    @Test
    void seCruzan_distintoDia_noSeCruzan() {
        assertFalse(HorarioUtil.seCruzan(List.of("LUNES"), t(8, 0), t(16, 0), List.of("MARTES"), t(8, 0), t(16, 0)));
    }

    @Test
    void seCruzan_nocturnoInvadeElDiaSiguiente() {
        assertTrue(HorarioUtil.seCruzan(List.of("LUNES"), t(22, 0), t(6, 0), List.of("MARTES"), t(5, 0), t(13, 0)));
    }

    @Test
    void seCruzan_nocturnoDeDomingoInvadeElLunes() {
        assertTrue(HorarioUtil.seCruzan(List.of("DOMINGO"), t(22, 0), t(6, 0), List.of("LUNES"), t(5, 0), t(13, 0)));
        assertTrue(HorarioUtil.seCruzan(List.of("LUNES"), t(5, 0), t(13, 0), List.of("DOMINGO"), t(22, 0), t(6, 0)), "simétrico");
    }

    @Test
    void seCruzan_diasInvalidosSeIgnoran() {
        assertFalse(HorarioUtil.seCruzan(List.of("x"), t(8, 0), t(16, 0), List.of("LUNES"), t(8, 0), t(16, 0)));
    }
}
