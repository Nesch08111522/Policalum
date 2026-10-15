package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;

import com.policalum.erp.application.dto.response.FlujoCajaResumenDTO;
import com.policalum.erp.application.service.serviceImpl.MovimientosCaja.Concepto;
import com.policalum.erp.application.service.serviceImpl.MovimientosCaja.Movimiento;
import com.policalum.erp.domain.enums.MetodoPago;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Módulo 6: agregaciones puras del flujo de caja (sin mocks). */
class MovimientosCajaTest {

    UUID centro = UUID.randomUUID(), norte = UUID.randomUUID();

    private Movimiento mov(UUID sede, Concepto c, MetodoPago m, String monto) {
        return new Movimiento(sede, c, m, new BigDecimal(monto));
    }

    private MovimientosCaja caja() {
        return new MovimientosCaja(List.of(
                mov(centro, Concepto.COBRO_VENTA, MetodoPago.EFECTIVO, "1000.00"),
                mov(centro, Concepto.COBRO_VENTA, MetodoPago.YAPE, "500.00"),
                mov(norte, Concepto.COBRO_VENTA, MetodoPago.TRANSFERENCIA, "300.00"),
                mov(centro, Concepto.PAGO_PROVEEDOR, MetodoPago.TRANSFERENCIA, "400.00"),
                mov(centro, Concepto.PAGO_NOMINA, MetodoPago.EFECTIVO, "200.00"),
                mov(norte, Concepto.REEMBOLSO, MetodoPago.EFECTIVO, "50.00"),
                mov(null, Concepto.OTRO_PAGO, null, "30.00"),
                mov(centro, Concepto.RETIRO_SOCIO, MetodoPago.EFECTIVO, "100.00")));
    }

    @Test
    void concepto_soloCobroVentaEsIngreso() {
        assertTrue(Concepto.COBRO_VENTA.esIngreso());
        for (Concepto c : Concepto.values()) {
            if (c != Concepto.COBRO_VENTA) assertFalse(c.esIngreso(), c.name());
        }
    }

    @Test
    void movimiento_conSignoYEsEfectivo() {
        assertEquals(new BigDecimal("10"), mov(centro, Concepto.COBRO_VENTA, MetodoPago.EFECTIVO, "10").conSigno());
        assertEquals(new BigDecimal("-10"), mov(centro, Concepto.PAGO_NOMINA, MetodoPago.EFECTIVO, "10").conSigno());
        assertTrue(mov(centro, Concepto.COBRO_VENTA, MetodoPago.EFECTIVO, "1").esEfectivo());
        assertFalse(mov(centro, Concepto.COBRO_VENTA, null, "1").esEfectivo(), "sin método se trata como digital");
    }

    @Test
    void constructor_copiaDefensiva() {
        List<Movimiento> lista = new ArrayList<>(List.of(mov(centro, Concepto.COBRO_VENTA, MetodoPago.EFECTIVO, "1")));
        MovimientosCaja c = new MovimientosCaja(lista);
        lista.clear();

        assertFalse(c.estaVacio());
        assertThrows(UnsupportedOperationException.class, () -> c.items().add(null));
    }

    @Test
    void vacio_todoEnCero() {
        MovimientosCaja c = new MovimientosCaja(List.of());

        assertTrue(c.estaVacio());
        assertTrue(c.sedes().isEmpty());
        assertEquals(new BigDecimal("0.00"), c.neto(m -> true));
        assertEquals(new BigDecimal("0.00"), c.resumen().flujoNeto().total());
        assertTrue(c.resumen().cobrosPorMetodo().isEmpty());
    }

    @Test
    void sedes_ignoraNulos() {
        assertEquals(Set.of(centro, norte), caja().sedes());
    }

    @Test
    void neto_totalYEgresosSinMetodo() {
        MovimientosCaja c = caja();

        // 1800 cobros − 400 − 200 − 50 − 30 − 100 = 1020
        assertEquals(new BigDecimal("1020.00"), c.neto(m -> true));
        assertEquals(new BigDecimal("1800.00"), c.total(Concepto.COBRO_VENTA));
        assertEquals(new BigDecimal("0.00"), c.total(Concepto.PAGO_NOMINA).subtract(new BigDecimal("200.00")));
        assertEquals(new BigDecimal("30.00"), c.egresosSinMetodo());
    }

    @Test
    void resumen_global_separaEfectivoDigitalYRetiros() {
        FlujoCajaResumenDTO r = caja().resumen();

        assertEquals(new BigDecimal("1000.00"), r.cobrosVentas().efectivo());
        assertEquals(new BigDecimal("800.00"), r.cobrosVentas().digital());
        assertEquals(new BigDecimal("680.00"), r.totalEgresosOperativos().total());   // 400 + 200 + 50 + 30
        assertEquals(new BigDecimal("1120.00"), r.flujoOperativo().total());          // 1800 − 680
        assertEquals(new BigDecimal("750.00"), r.flujoOperativo().efectivo());        // 1000 − 200 − 50
        assertEquals(new BigDecimal("100.00"), r.retirosSocios().total());
        assertEquals(new BigDecimal("1020.00"), r.flujoNeto().total());
        assertEquals(Map.of("EFECTIVO", new BigDecimal("1000.00"), "YAPE", new BigDecimal("500.00"),
                "TRANSFERENCIA", new BigDecimal("300.00")), r.cobrosPorMetodo());
    }

    @Test
    void resumenSede_soloMovimientosDeEsaSede() {
        FlujoCajaResumenDTO r = caja().resumenSede(norte);

        assertEquals(new BigDecimal("300.00"), r.cobrosVentas().total());
        assertEquals(new BigDecimal("50.00"), r.reembolsosClientes().total());
        assertEquals(new BigDecimal("0.00"), r.otrosPagos().total(), "el pago sin sede no se atribuye a ninguna sede");
        assertEquals(new BigDecimal("250.00"), r.flujoNeto().total());
        assertEquals(Set.of("TRANSFERENCIA"), r.cobrosPorMetodo().keySet());
    }

    @Test
    void resumenSede_nula_tomaSoloLosSinSede() {
        FlujoCajaResumenDTO r = caja().resumenSede(null);

        assertEquals(new BigDecimal("30.00"), r.otrosPagos().total());
        assertEquals(new BigDecimal("-30.00"), r.flujoNeto().total());
    }
}
