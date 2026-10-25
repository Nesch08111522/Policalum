package com.policalum.erp.infrastructure.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** PDF con cabecera de empresa, filas cebra y pie de página. */
public final class PdfCorporativo {

    private static final Color AZUL = new Color(7, 85, 145);
    private static final Color CEBRA = new Color(244, 247, 251);
    private static final Color GRIS = new Color(90, 104, 120);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private PdfCorporativo() {}

    public static byte[] generar(ReporteSolicitud reporte, EmpresaPdf empresa) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = new Document(PageSize.A4.rotate(), 36, 36, 78, 48);
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            writer.setPageEvent(new Pie(empresa, reporte.usuario()));
            documento.open();
            documento.add(new Paragraph(reporte.titulo(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, AZUL)));
            Font meta = FontFactory.getFont(FontFactory.HELVETICA, 9, GRIS);
            for (String filtro : reporte.filtros()) {
                documento.add(new Paragraph(filtro, meta));
            }
            documento.add(new Paragraph(" "));
            PdfPTable tabla = new PdfPTable(Math.max(reporte.columnas().size(), 1));
            tabla.setWidthPercentage(100);
            tabla.setHeaderRows(1);
            for (ReporteSolicitud.Columna columna : reporte.columnas()) {
                tabla.addCell(cabecera(columna.titulo()));
            }
            int indice = 0;
            for (List<Object> fila : reporte.filas()) {
                Color fondo = indice++ % 2 == 0 ? Color.WHITE : CEBRA;
                for (int c = 0; c < reporte.columnas().size(); c++) {
                    Object valor = c < fila.size() ? fila.get(c) : null;
                    tabla.addCell(dato(presentar(valor, reporte.columnas().get(c).tipo()), fondo, reporte.columnas().get(c).tipo()));
                }
            }
            documento.add(tabla);
            documento.close();
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    public static String presentar(Object valor, ReporteSolicitud.Tipo tipo) {
        if (valor == null) return "";
        if (tipo == ReporteSolicitud.Tipo.MONEDA && valor instanceof BigDecimal monto) {
            return "S/ " + String.format(java.util.Locale.US, "%,.2f", monto);
        }
        if (tipo == ReporteSolicitud.Tipo.FECHA && valor instanceof LocalDateTime momento) {
            return FECHA.format(momento);
        }
        return String.valueOf(valor);
    }

    private static PdfPCell cabecera(String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        celda.setBackgroundColor(AZUL);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(6);
        celda.setBorderColor(AZUL);
        return celda;
    }

    private static PdfPCell dato(String texto, Color fondo, ReporteSolicitud.Tipo tipo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK)));
        celda.setBackgroundColor(fondo);
        celda.setPadding(5);
        celda.setBorderColor(new Color(226, 232, 240));
        if (tipo == ReporteSolicitud.Tipo.MONEDA) celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return celda;
    }

    public record EmpresaPdf(String nombre, String razonSocial, String ruc, String direccion, byte[] logo) {}

    private static final class Pie extends PdfPageEventHelper {
        private final EmpresaPdf empresa;
        private final String usuario;
        private PdfTemplate total;

        private Pie(EmpresaPdf empresa, String usuario) {
            this.empresa = empresa;
            this.usuario = usuario == null ? "" : usuario;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(30, 14);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte fondo = writer.getDirectContentUnder();
            fondo.saveState();
            fondo.setColorFill(new Color(7, 85, 145, 18));
            fondo.beginText();
            fondo.setFontAndSize(FontFactory.getFont(FontFactory.HELVETICA_BOLD).getBaseFont(), 42);
            fondo.showTextAligned(Element.ALIGN_CENTER, "POLICALUM ERP", document.getPageSize().getWidth() / 2, 220, 32);
            fondo.endText();
            fondo.restoreState();

            PdfContentByte cb = writer.getDirectContent();
            float arriba = document.getPageSize().getHeight() - 28;
            if (empresa.logo() != null && empresa.logo().length > 0) {
                try {
                    Image logo = Image.getInstance(empresa.logo());
                    logo.scaleToFit(42, 42);
                    logo.setAbsolutePosition(document.leftMargin(), arriba - 18);
                    cb.addImage(logo);
                } catch (Exception ignored) {
                    // La cabecera sigue siendo legible sin el archivo del logo.
                }
            }
            Font marca = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, AZUL);
            Font chico = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(empresa.razonSocial(), marca),
                    document.getPageSize().getWidth() / 2, arriba, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase("RUC " + empresa.ruc(), chico),
                    document.getPageSize().getWidth() / 2, arriba - 14, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(FECHA.format(LocalDateTime.now()), chico),
                    document.right(), arriba, 0);

            String pie = "Página " + writer.getPageNumber() + " de ";
            Font pieFuente = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);
            float y = document.bottom() - 24;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(pie, pieFuente), document.leftMargin(), y, 0);
            cb.addTemplate(total, document.leftMargin() + pieFuente.getBaseFont().getWidthPoint(pie, 8), y);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(usuario, pieFuente), document.right(), y, 0);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(total, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(writer.getPageNumber() - 1), FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS)),
                    0, 0, 0);
        }
    }

}
