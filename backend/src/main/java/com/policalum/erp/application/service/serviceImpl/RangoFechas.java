package com.policalum.erp.application.service.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rango de fechas inclusivo [desde, hasta] de los reportes financieros, convertido al
 * semiabierto [inicio, finExclusivo) que usan las consultas. Sin fechas: mes en curso.
 */
record RangoFechas(LocalDate desde, LocalDate hasta) {

    static RangoFechas de(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        LocalDate d = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : hoy;
        if (d.isAfter(h)) throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        return new RangoFechas(d, h);
    }

    LocalDateTime inicio() {
        return desde.atStartOfDay();
    }

    LocalDateTime finExclusivo() {
        return hasta.plusDays(1).atStartOfDay();
    }

    long dias() {
        return java.time.temporal.ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    /** El periodo inmediatamente anterior con la misma cantidad de días. */
    RangoFechas anterior() {
        LocalDate h = desde.minusDays(1);
        return new RangoFechas(h.minusDays(dias() - 1), h);
    }
}
