package com.policalum.erp.application.service.support;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PostgreSQL no infiere el tipo de un parámetro usado en {@code :fecha IS NULL},
 * aunque el valor Java no sea nulo. Los listados comparan siempre contra estos
 * límites cuando el filtro de fecha viene vacío.
 */
public final class RangosConsulta {

    public static final LocalDateTime DESDE_ABIERTO = LocalDateTime.of(1970, 1, 1, 0, 0);
    public static final LocalDateTime HASTA_ABIERTO = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
    public static final LocalDate FECHA_DESDE_ABIERTA = LocalDate.of(1970, 1, 1);
    public static final LocalDate FECHA_HASTA_ABIERTA = LocalDate.of(9999, 12, 31);

    private RangosConsulta() {
    }

    public static LocalDateTime desde(LocalDateTime value) {
        return value != null ? value : DESDE_ABIERTO;
    }

    public static LocalDateTime hasta(LocalDateTime value) {
        return value != null ? value : HASTA_ABIERTO;
    }

    public static LocalDate desde(LocalDate value) {
        return value != null ? value : FECHA_DESDE_ABIERTA;
    }

    public static LocalDate hasta(LocalDate value) {
        return value != null ? value : FECHA_HASTA_ABIERTA;
    }
}
