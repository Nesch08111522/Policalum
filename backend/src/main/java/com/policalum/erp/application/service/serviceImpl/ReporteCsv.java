package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.ArqueoTurnoDTO;
import com.policalum.erp.application.dto.response.ClienteFrecuenteDTO;
import com.policalum.erp.application.dto.response.ProveedorRankingDTO;
import com.policalum.erp.application.dto.response.RentabilidadItemDTO;
import com.policalum.erp.application.dto.response.VentaReporteDTO;
import com.policalum.erp.util.CsvUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Columnas y filas CSV de los reportes (Módulo 12). Todo pasa por {@link CsvUtil}: UTF-8 con BOM,
 * RFC 4180 y protección contra inyección de fórmulas.
 */
final class ReporteCsv {

    static final String[] COLUMNAS_VENTAS = {"fecha", "serie_correlativo", "sede", "vendedor", "cliente", "documento_cliente",
            "canal", "tipo_comprobante", "estado", "neto", "igv", "total", "metodos_pago"};
    static final String[] COLUMNAS_RENTABILIDAD = {"tipo", "nombre", "sku", "categoria", "unidad", "ventas", "cantidad_vendida",
            "cantidad_devuelta", "cantidad_neta", "ingreso_neto", "devoluciones_neto", "ingreso_neto_real", "participacion_pct",
            "costo_unitario_actual", "costo_estimado", "margen", "margen_pct"};
    static final String[] COLUMNAS_CLIENTES = {"posicion", "nombre", "tipo_documento", "numero_documento", "compras",
            "monto_total", "devuelto", "monto_neto", "ticket_promedio", "primera_compra", "ultima_compra",
            "dias_entre_compras", "dias_desde_ultima_compra"};
    static final String[] COLUMNAS_PROVEEDORES = {"posicion", "nombre", "numero_documento", "compras", "recibidas", "pendientes",
            "anuladas", "monto_recibido_pen", "ticket_promedio", "ultima_compra", "dias_entrega_promedio", "dias_entrega_maximo"};
    static final String[] COLUMNAS_ARQUEOS = {"apertura", "cierre", "sede", "cajero", "estado", "monto_inicial", "ventas_efectivo",
            "retiros_socios", "reembolsos", "esperado", "declarado", "diferencia", "resultado", "efectivo_ventas_anuladas"};

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ReporteCsv() { }

    /** Nuevo CSV con BOM y fila de encabezados. */
    static StringBuilder nuevo(String[] columnas) {
        StringBuilder sb = new StringBuilder(CsvUtil.BOM);
        CsvUtil.linea(sb, columnas);
        return sb;
    }

    static void venta(StringBuilder sb, VentaReporteDTO v) {
        CsvUtil.linea(sb, fecha(v.fecha()), v.serieCorrelativo(), v.sede(), v.vendedor(), v.cliente(), v.clienteDocumento(),
                nombre(v.canal()), nombre(v.tipoComprobante()), nombre(v.estado()), CsvUtil.numero(v.neto()),
                CsvUtil.numero(v.igv()), CsvUtil.numero(v.total()), v.metodosPago());
    }

    static void rentabilidad(StringBuilder sb, List<RentabilidadItemDTO> items) {
        for (RentabilidadItemDTO i : items) {
            CsvUtil.linea(sb, i.tipo(), i.nombre(), i.sku(), i.categoria(), i.unidad(), String.valueOf(i.ventas()),
                    CsvUtil.numero(i.cantidadVendida()), CsvUtil.numero(i.cantidadDevuelta()), CsvUtil.numero(i.cantidadNeta()),
                    CsvUtil.numero(i.ingresoNeto()), CsvUtil.numero(i.devolucionesNeto()), CsvUtil.numero(i.ingresoNetoReal()),
                    CsvUtil.numero(i.participacionPorcentaje()), CsvUtil.numero(i.costoUnitario()),
                    CsvUtil.numero(i.costoEstimado()), CsvUtil.numero(i.margen()), CsvUtil.numero(i.margenPorcentaje()));
        }
    }

    static void clientes(StringBuilder sb, List<ClienteFrecuenteDTO> items) {
        for (ClienteFrecuenteDTO c : items) {
            CsvUtil.linea(sb, String.valueOf(c.posicion()), c.nombre(), c.tipoDocumento(), c.numeroDocumento(),
                    String.valueOf(c.compras()), CsvUtil.numero(c.montoTotal()), CsvUtil.numero(c.devuelto()),
                    CsvUtil.numero(c.montoNeto()), CsvUtil.numero(c.ticketPromedio()), fecha(c.primeraCompra()),
                    fecha(c.ultimaCompra()), CsvUtil.numero(c.diasEntreCompras()), String.valueOf(c.diasDesdeUltimaCompra()));
        }
    }

    static void proveedores(StringBuilder sb, List<ProveedorRankingDTO> items) {
        for (ProveedorRankingDTO p : items) {
            CsvUtil.linea(sb, String.valueOf(p.posicion()), p.nombre(), p.numeroDocumento(), String.valueOf(p.compras()),
                    String.valueOf(p.recibidas()), String.valueOf(p.pendientes()), String.valueOf(p.anuladas()),
                    CsvUtil.numero(p.montoRecibido()), CsvUtil.numero(p.ticketPromedio()), fecha(p.ultimaCompra()),
                    CsvUtil.numero(p.diasEntregaPromedio()), p.diasEntregaMaximo() != null ? String.valueOf(p.diasEntregaMaximo()) : null);
        }
    }

    static void arqueos(StringBuilder sb, List<ArqueoTurnoDTO> items) {
        for (ArqueoTurnoDTO a : items) {
            CsvUtil.linea(sb, fecha(a.apertura()), fecha(a.cierre()), a.sede(), a.cajero(), nombre(a.estado()),
                    CsvUtil.numero(a.montoInicial()), CsvUtil.numero(a.ventasEfectivo()), CsvUtil.numero(a.retirosSocios()),
                    CsvUtil.numero(a.reembolsos()), CsvUtil.numero(a.esperado()), CsvUtil.numero(a.declarado()),
                    CsvUtil.numero(a.diferencia()), nombre(a.resultado()), CsvUtil.numero(a.efectivoVentasAnuladas()));
        }
    }

    private static String fecha(LocalDateTime f) {
        return f != null ? f.format(FECHA_HORA) : null;
    }

    private static String fecha(LocalDate f) {
        return f != null ? f.toString() : null;
    }

    private static String nombre(Enum<?> e) {
        return e != null ? e.name() : null;
    }
}
