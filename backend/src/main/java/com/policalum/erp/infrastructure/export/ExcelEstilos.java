package com.policalum.erp.infrastructure.export;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Libro .xlsx con cabecera corporativa, moneda, fechas y fila congelada. */
public final class ExcelEstilos {

    private static final byte[] AZUL = new byte[] { 7, 85, (byte) 145 };

    private ExcelEstilos() {}

    public static byte[] generar(ReporteSolicitud reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            resumen(libro, reporte);
            datos(libro, reporte);
            libro.write(salida);
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el Excel", e);
        }
    }

    private static void resumen(XSSFWorkbook libro, ReporteSolicitud reporte) {
        Sheet hoja = libro.createSheet("Resumen");
        CellStyle titulo = titulo(libro);
        int fila = 0;
        celda(hoja, fila++, 0, reporte.titulo(), titulo);
        celda(hoja, fila++, 0, "Módulo", null);
        celda(hoja, fila - 1, 1, reporte.modulo(), null);
        celda(hoja, fila++, 0, "Exportado", null);
        celda(hoja, fila - 1, 1, LocalDateTime.now().toString().replace('T', ' ').substring(0, 16), null);
        celda(hoja, fila++, 0, "Usuario", null);
        celda(hoja, fila - 1, 1, reporte.usuario() == null ? "" : reporte.usuario(), null);
        for (String filtro : reporte.filtros()) {
            celda(hoja, fila++, 0, filtro, null);
        }
        celda(hoja, fila, 0, "Filas", null);
        celda(hoja, fila, 1, String.valueOf(reporte.filas().size()), null);
        hoja.setColumnWidth(0, 28 * 256);
        hoja.setColumnWidth(1, 48 * 256);
    }

    private static void datos(XSSFWorkbook libro, ReporteSolicitud reporte) {
        Sheet hoja = libro.createSheet("Datos");
        CellStyle cabecera = cabecera(libro);
        CellStyle moneda = moneda(libro);
        CellStyle fecha = fecha(libro);
        CellStyle texto = texto(libro);
        Row encabezado = hoja.createRow(0);
        encabezado.setHeightInPoints(22);
        List<ReporteSolicitud.Columna> columnas = reporte.columnas();
        for (int i = 0; i < columnas.size(); i++) {
            Cell celda = encabezado.createCell(i);
            celda.setCellValue(columnas.get(i).titulo());
            celda.setCellStyle(cabecera);
        }
        int r = 1;
        for (List<Object> valores : reporte.filas()) {
            Row row = hoja.createRow(r++);
            for (int c = 0; c < columnas.size(); c++) {
                Object valor = c < valores.size() ? valores.get(c) : null;
                Cell celda = row.createCell(c);
                ReporteSolicitud.Tipo tipo = columnas.get(c).tipo();
                if (tipo == ReporteSolicitud.Tipo.MONEDA) {
                    BigDecimal monto = valor instanceof BigDecimal decimal ? decimal : BigDecimal.ZERO;
                    celda.setCellValue(monto.doubleValue());
                    celda.setCellStyle(moneda);
                } else if (tipo == ReporteSolicitud.Tipo.FECHA && valor instanceof LocalDateTime momento) {
                    celda.setCellValue(Date.from(momento.atZone(ZoneId.systemDefault()).toInstant()));
                    celda.setCellStyle(fecha);
                } else {
                    celda.setCellValue(valor == null ? "" : String.valueOf(valor));
                    celda.setCellStyle(texto);
                }
            }
        }
        hoja.createFreezePane(0, 1);
        hoja.setAutoFilter(new CellRangeAddress(0, Math.max(r - 1, 1), 0, Math.max(columnas.size() - 1, 0)));
        for (int i = 0; i < columnas.size(); i++) {
            try {
                hoja.autoSizeColumn(i);
            } catch (RuntimeException ignored) {
                hoja.setColumnWidth(i, 18 * 256);
            }
            int ancho = hoja.getColumnWidth(i);
            hoja.setColumnWidth(i, Math.min(Math.max(ancho + 512, 12 * 256), 48 * 256));
        }
    }

    private static void celda(Sheet hoja, int fila, int columna, String valor, CellStyle estilo) {
        Row row = hoja.getRow(fila);
        if (row == null) row = hoja.createRow(fila);
        Cell celda = row.createCell(columna);
        celda.setCellValue(valor == null ? "" : valor);
        if (estilo != null) celda.setCellStyle(estilo);
    }

    private static CellStyle cabecera(XSSFWorkbook libro) {
        XSSFCellStyle estilo = libro.createCellStyle();
        estilo.setFillForegroundColor(new XSSFColor(AZUL, null));
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        bordes(estilo);
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        return estilo;
    }

    private static CellStyle titulo(XSSFWorkbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 14);
        estilo.setFont(fuente);
        return estilo;
    }

    private static CellStyle moneda(XSSFWorkbook libro) {
        CellStyle estilo = texto(libro);
        estilo.setDataFormat(libro.createDataFormat().getFormat("\"S/\" #,##0.00"));
        estilo.setAlignment(HorizontalAlignment.RIGHT);
        return estilo;
    }

    private static CellStyle fecha(XSSFWorkbook libro) {
        CellStyle estilo = texto(libro);
        estilo.setDataFormat(libro.createDataFormat().getFormat("dd/MM/yyyy HH:mm"));
        return estilo;
    }

    private static CellStyle texto(XSSFWorkbook libro) {
        CellStyle estilo = libro.createCellStyle();
        bordes(estilo);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        return estilo;
    }

    private static void bordes(CellStyle estilo) {
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
    }
}
