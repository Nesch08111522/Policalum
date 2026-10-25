package com.policalum.erp.infrastructure.export;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Datos ya resueltos para Excel o PDF. Las celdas son texto, moneda o fecha. */
public record ReporteSolicitud(
        String titulo,
        String modulo,
        String usuario,
        List<String> filtros,
        List<Columna> columnas,
        List<List<Object>> filas
) {
    public enum Tipo { TEXTO, MONEDA, FECHA }

    public record Columna(String titulo, Tipo tipo) {
        public static Columna texto(String titulo) { return new Columna(titulo, Tipo.TEXTO); }
        public static Columna moneda(String titulo) { return new Columna(titulo, Tipo.MONEDA); }
        public static Columna fecha(String titulo) { return new Columna(titulo, Tipo.FECHA); }
    }

    public static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    public static BigDecimal dinero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    public static LocalDateTime fecha(LocalDateTime valor) {
        return valor;
    }
}
