package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.FlujoCajaResumenDTO;
import com.policalum.erp.application.dto.response.MontoPorMedioDTO;
import com.policalum.erp.domain.enums.MetodoPago;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Movimientos de dinero real de un rango de fechas, ya agrupados por sede, concepto y
 * método de pago (Módulo 6). Lo arma {@link MovimientosCajaCalculator}; aquí solo se suma.
 * Todos los montos son positivos; el concepto dice si es ingreso o egreso.
 */
public record MovimientosCaja(List<Movimiento> items) {

    public enum Concepto {
        COBRO_VENTA, PAGO_PROVEEDOR, PAGO_NOMINA, REEMBOLSO, OTRO_PAGO, RETIRO_SOCIO;

        public boolean esIngreso() {
            return this == COBRO_VENTA;
        }
    }

    /** metodo null = no se registró el método (se trata como medio digital). */
    public record Movimiento(UUID sedeId, Concepto concepto, MetodoPago metodo, BigDecimal monto) {
        public BigDecimal conSigno() {
            return concepto.esIngreso() ? monto : monto.negate();
        }

        public boolean esEfectivo() {
            return metodo == MetodoPago.EFECTIVO;
        }
    }

    public MovimientosCaja {
        items = List.copyOf(items);
    }

    public boolean estaVacio() {
        return items.isEmpty();
    }

    /** Sedes que tuvieron al menos un movimiento. */
    public Set<UUID> sedes() {
        return items.stream().map(Movimiento::sedeId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /** Ingresos - egresos de los movimientos que cumplen el filtro. */
    public BigDecimal neto(Predicate<Movimiento> filtro) {
        return items.stream().filter(filtro).map(Movimiento::conSigno).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Suma de un concepto (todas las sedes del rango cargado). */
    public BigDecimal total(Concepto concepto) {
        return items.stream().filter(m -> m.concepto() == concepto).map(Movimiento::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    /** Egresos sin método de pago registrado (se cuentan como digitales). */
    public BigDecimal egresosSinMetodo() {
        return items.stream().filter(m -> m.metodo() == null && !m.concepto().esIngreso()).map(Movimiento::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    public FlujoCajaResumenDTO resumen() {
        return resumen(m -> true);
    }

    public FlujoCajaResumenDTO resumenSede(UUID sedeId) {
        return resumen(m -> Objects.equals(sedeId, m.sedeId()));
    }

    private FlujoCajaResumenDTO resumen(Predicate<Movimiento> filtro) {
        MontoPorMedioDTO cobros = porMedio(filtro, Concepto.COBRO_VENTA);
        MontoPorMedioDTO proveedores = porMedio(filtro, Concepto.PAGO_PROVEEDOR);
        MontoPorMedioDTO nominas = porMedio(filtro, Concepto.PAGO_NOMINA);
        MontoPorMedioDTO reembolsos = porMedio(filtro, Concepto.REEMBOLSO);
        MontoPorMedioDTO otros = porMedio(filtro, Concepto.OTRO_PAGO);
        MontoPorMedioDTO retiros = porMedio(filtro, Concepto.RETIRO_SOCIO);
        MontoPorMedioDTO egresos = proveedores.mas(nominas).mas(reembolsos).mas(otros);
        MontoPorMedioDTO operativo = cobros.menos(egresos);
        return new FlujoCajaResumenDTO(cobros, cobrosPorMetodo(filtro), proveedores, nominas, reembolsos, otros,
                egresos, operativo, retiros, operativo.menos(retiros));
    }

    private MontoPorMedioDTO porMedio(Predicate<Movimiento> filtro, Concepto concepto) {
        BigDecimal efectivo = BigDecimal.ZERO, digital = BigDecimal.ZERO;
        for (Movimiento m : items) {
            if (m.concepto() != concepto || !filtro.test(m)) continue;
            if (m.esEfectivo()) efectivo = efectivo.add(m.monto());
            else digital = digital.add(m.monto());
        }
        return MontoPorMedioDTO.de(efectivo, digital);
    }

    /** Cobros por método en el orden del enum; solo métodos con monto. */
    private Map<String, BigDecimal> cobrosPorMetodo(Predicate<Movimiento> filtro) {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        for (MetodoPago metodo : MetodoPago.values()) {
            BigDecimal suma = items.stream()
                    .filter(m -> m.concepto() == Concepto.COBRO_VENTA && m.metodo() == metodo && filtro.test(m))
                    .map(Movimiento::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (suma.signum() != 0) mapa.put(metodo.name(), suma.setScale(2, RoundingMode.HALF_UP));
        }
        return mapa;
    }
}
