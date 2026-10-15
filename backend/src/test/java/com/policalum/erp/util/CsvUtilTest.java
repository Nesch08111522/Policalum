package com.policalum.erp.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Exportaciones CSV: escapado RFC 4180 y neutralización de fórmulas (CSV injection). */
class CsvUtilTest {

    @Test
    void celda_nulaOVacia_quedaVacia() {
        assertEquals("", CsvUtil.celda(null));
        assertEquals("", CsvUtil.celda(""));
    }

    @Test
    void celda_textoSimple_sinCambios() {
        assertEquals("Policarbonato 6mm", CsvUtil.celda("Policarbonato 6mm"));
    }

    @Test
    void celda_conComaComillasOSaltos_vaEntreComillasYDuplicaComillas() {
        assertEquals("\"Lima, Perú\"", CsvUtil.celda("Lima, Perú"));
        assertEquals("\"Plancha 6\"\" x 2\"", CsvUtil.celda("Plancha 6\" x 2"));
        assertEquals("\"línea1\nlínea2\"", CsvUtil.celda("línea1\nlínea2"));
    }

    @Test
    void celda_espaciosAlBorde_seConservanEntreComillas() {
        assertEquals("\" Ana \"", CsvUtil.celda(" Ana "));
    }

    @Test
    void celda_empiezaConCaracterDeFormula_seNeutralizaConApostrofe() {
        assertEquals("'=SUM(A1:A9)", CsvUtil.celda("=SUM(A1:A9)"));
        assertEquals("'+51987654321", CsvUtil.celda("+51987654321"));
        assertEquals("'-5", CsvUtil.celda("-5"));
        assertEquals("'@cmd", CsvUtil.celda("@cmd"));
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", CsvUtil.celda("=HYPERLINK(\"x\")"));
    }

    @Test
    void linea_separaConComasYTerminaEnCrlf() {
        StringBuilder sb = new StringBuilder();

        CsvUtil.linea(sb, "a", null, "b,c");

        assertEquals("a,,\"b,c\"\r\n", sb.toString());
    }

    @Test
    void numero_sinCerosNiNotacionCientifica() {
        assertEquals("", CsvUtil.numero(null));
        assertEquals("118", CsvUtil.numero(new BigDecimal("118.00")));
        assertEquals("0.5", CsvUtil.numero(new BigDecimal("0.5000")));
        assertEquals("1000000", CsvUtil.numero(new BigDecimal("1E+6")));
    }
}
