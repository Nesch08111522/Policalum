package com.policalum.erp.util;

/**
 * CSV para Excel sin librerías: UTF-8 con BOM, separado por comas, RFC 4180, y protección
 * contra inyección de fórmulas (un valor que empieza con = + - @ se antepone con ').
 */
public final class CsvUtil {

    /** Marca de orden de bytes: hace que Excel lea el archivo como UTF-8 (tildes, ñ). */
    public static final String BOM = "\uFEFF";

    private CsvUtil() { }

    /** Agrega una línea terminada en CRLF. */
    public static void linea(StringBuilder sb, String... valores) {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(celda(valores[i]));
        }
        sb.append("\r\n");
    }

    /** Escapa un valor. null o vacío = celda vacía. */
    public static String celda(String valor) {
        if (valor == null || valor.isEmpty()) return "";
        String v = valor;
        char primero = v.charAt(0);
        if (primero == '=' || primero == '+' || primero == '-' || primero == '@' || primero == '\t' || primero == '\r') {
            v = "'" + v;
        }
        boolean comillas = v.indexOf(',') >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0
                || !v.equals(v.trim());
        return comillas ? '"' + v.replace("\"", "\"\"") + '"' : v;
    }

    /** Número sin notación científica, apto para la columna numérica de Excel (no se antepone '). */
    public static String numero(java.math.BigDecimal valor) {
        return valor == null ? "" : valor.stripTrailingZeros().toPlainString();
    }
}
