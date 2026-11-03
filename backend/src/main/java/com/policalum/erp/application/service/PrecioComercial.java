package com.policalum.erp.application.service;

import com.policalum.erp.domain.model.Producto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;

/** Precio que se cobra hoy. Si hay oferta vigente, manda el precio de oferta. */
public final class PrecioComercial {

    public static final ZoneId LIMA = ZoneId.of("America/Lima");

    private PrecioComercial() {
    }

    public static boolean vigente(Producto producto) {
        if (producto == null || producto.getPrecioOferta() == null || producto.getOfertaDesde() == null || producto.getOfertaHasta() == null) {
            return false;
        }
        LocalDate hoy = LocalDate.now(LIMA);
        return !hoy.isBefore(producto.getOfertaDesde()) && !hoy.isAfter(producto.getOfertaHasta());
    }

    public static boolean vigente(Producto producto, LocalDate hoy) {
        if (producto == null || producto.getPrecioOferta() == null || producto.getOfertaDesde() == null || producto.getOfertaHasta() == null) {
            return false;
        }
        return !hoy.isBefore(producto.getOfertaDesde()) && !hoy.isAfter(producto.getOfertaHasta());
    }

    public static BigDecimal venta(Producto producto) {
        if (producto == null) return null;
        return vigente(producto) ? producto.getPrecioOferta() : producto.getPrecioVentaBase();
    }

    public static BigDecimal fraccion(Producto producto, BigDecimal precioLista) {
        if (precioLista == null || producto == null || !vigente(producto)) return precioLista;
        BigDecimal base = producto.getPrecioVentaBase();
        if (base == null || base.signum() == 0 || producto.getPrecioOferta() == null) return precioLista;
        return precioLista.multiply(producto.getPrecioOferta()).divide(base, 2, RoundingMode.HALF_UP);
    }

    /** Oferta con precio vigente, o la marca antigua sin cambio de precio. */
    public static boolean mostrar(Producto producto) {
        return vigente(producto) || (producto != null && Boolean.TRUE.equals(producto.getOferta()) && producto.getPrecioOferta() == null);
    }
}
