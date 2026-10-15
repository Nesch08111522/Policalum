package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.response.DisponibleCalculadoDTO;
import com.policalum.erp.application.dto.response.FlujoCajaResumenDTO;
import com.policalum.erp.application.dto.response.SaldoCuentaEstimadoDTO;
import com.policalum.erp.application.service.serviceImpl.MovimientosCaja.Concepto;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.CuentaFinanciera;
import com.policalum.erp.domain.model.SaldoCuentaFinanciera;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.infrastructure.repository.projection.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovimientosCajaCalculatorTest {

    @Mock PagoVentaRepository pagoVentaRepository;
    @Mock PagoCuentaPorPagarRepository pagoCxpRepository;
    @Mock FinanzasCuentaPorPagarRepository cxpRepository;
    @Mock RrhhNominaRepository nominaRepository;
    @Mock ReembolsoRepository reembolsoRepository;
    @Mock FinanzasRetiroSocioRepository retiroRepository;
    @Mock CuentaFinancieraRepository cuentaRepository;
    @Mock SaldoCuentaFinancieraRepository saldoRepository;
    @InjectMocks MovimientosCajaCalculator calculator;

    UUID centro = UUID.randomUUID(), norte = UUID.randomUUID();
    LocalDateTime desde = LocalDateTime.of(2026, 10, 1, 0, 0), hasta = LocalDateTime.of(2026, 11, 1, 0, 0);

    @BeforeEach
    void setUp() {
        lenient().when(pagoVentaRepository.cobrosPorSedeYMetodo(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(pagoCxpRepository.pagosPorSede(any(), any(), any())).thenReturn(List.of());
        lenient().when(cxpRepository.pagosSinDetallePorSede(any(), any(), any())).thenReturn(List.of());
        lenient().when(nominaRepository.pagadasPorSedeYMetodo(any(), any(), any())).thenReturn(List.of());
        lenient().when(reembolsoRepository.procesadosPorSedeYMetodo(any(), any(), any())).thenReturn(List.of());
        lenient().when(retiroRepository.totalesPorSedeYOrigen(any(), any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void movimientos_resumenSeparaEfectivoYDigitalYCalculaFlujos() {
        // given: cobros 1000 efectivo + 500 Yape; proveedor 300 transferencia; nómina 200 sin método;
        //        reembolso 50 efectivo; otro pago (servicio) 40; retiro de caja 100
        when(pagoVentaRepository.cobrosPorSedeYMetodo(EstadoVenta.COMPLETADA, desde, hasta, null)).thenReturn(List.of(
                cobro(centro, MetodoPago.EFECTIVO, "1000.00"), cobro(norte, MetodoPago.YAPE, "500.00")));
        when(pagoCxpRepository.pagosPorSede(desde, hasta, null)).thenReturn(List.of(
                pagoCxp(centro, TipoReferenciaCxP.COMPRA, MetodoPago.TRANSFERENCIA, "300.00"),
                pagoCxp(centro, TipoReferenciaCxP.SERVICIO, MetodoPago.EFECTIVO, "40.00")));
        when(nominaRepository.pagadasPorSedeYMetodo(desde, hasta, null)).thenReturn(List.of(cobro(norte, null, "200.00")));
        when(reembolsoRepository.procesadosPorSedeYMetodo(desde, hasta, null)).thenReturn(List.of(reembolso(centro, MetodoReembolso.EFECTIVO, "50.00")));
        when(retiroRepository.totalesPorSedeYOrigen(EstadoRetiro.REGISTRADO, desde, hasta, null)).thenReturn(List.of(retiro(centro, OrigenFondos.CAJA_FISICA, "100.00")));

        // when
        MovimientosCaja mov = calculator.movimientos(desde, hasta, null);
        FlujoCajaResumenDTO r = mov.resumen();

        // then
        assertEquals(new BigDecimal("1500.00"), r.cobrosVentas().total());
        assertEquals(new BigDecimal("1000.00"), r.cobrosVentas().efectivo());
        assertEquals(Map.of("EFECTIVO", new BigDecimal("1000.00"), "YAPE", new BigDecimal("500.00")), r.cobrosPorMetodo());
        assertEquals(new BigDecimal("300.00"), r.pagosProveedores().digital());
        assertEquals(new BigDecimal("40.00"), r.otrosPagos().efectivo());
        assertEquals(new BigDecimal("200.00"), r.pagosNominas().digital());
        assertEquals(new BigDecimal("590.00"), r.totalEgresosOperativos().total());
        assertEquals(new BigDecimal("910.00"), r.flujoOperativo().total());
        assertEquals(new BigDecimal("810.00"), r.flujoNeto().total());
        assertEquals(new BigDecimal("810.00"), r.flujoNeto().efectivo().add(r.flujoNeto().digital()));
        assertEquals(new BigDecimal("200.00"), mov.egresosSinMetodo());
        assertEquals(Set.of(centro, norte), mov.sedes());
        assertEquals(new BigDecimal("500.00"), mov.resumenSede(norte).cobrosVentas().total());
    }

    @Test
    void movimientos_cxpAntiguaSinDetalleYRetiroBancario_seMapeanAMetodos() {
        when(cxpRepository.pagosSinDetallePorSede(desde, hasta, centro)).thenReturn(List.of(
                pagoHistorico(centro, TipoReferenciaCxP.COMPRA, "YAPE", "120.00"),
                pagoHistorico(centro, TipoReferenciaCxP.OTRO, "texto raro", "30.00")));
        when(retiroRepository.totalesPorSedeYOrigen(EstadoRetiro.REGISTRADO, desde, hasta, centro))
                .thenReturn(List.of(retiro(centro, OrigenFondos.CUENTA_BANCARIA, "500.00")));

        MovimientosCaja mov = calculator.movimientos(desde, hasta, centro);

        assertEquals(new BigDecimal("120.00"), mov.total(Concepto.PAGO_PROVEEDOR));
        assertEquals(new BigDecimal("-120.00"), mov.neto(m -> m.metodo() == MetodoPago.YAPE));
        assertEquals(new BigDecimal("-500.00"), mov.neto(m -> m.metodo() == MetodoPago.TRANSFERENCIA));
        assertEquals(new BigDecimal("30.00"), mov.egresosSinMetodo());
    }

    @Test
    void disponible_sumaCorteMasMovimientosDeSusMetodosDesdeElCorte() {
        // given: BCP (corporativa) recibe YAPE y TRANSFERENCIA; corte S/ 5,000 hace 3 días
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 8, 18, 0);
        LocalDateTime fechaCorte = ahora.minusDays(3);
        CuentaFinanciera bcp = cuenta("BCP", null, MetodoPago.YAPE, MetodoPago.TRANSFERENCIA);
        when(cuentaRepository.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of(bcp));
        when(saldoRepository.findFirstByCuenta_IdOrderByFechaCorteDescCreatedAtDesc(bcp.getId()))
                .thenReturn(Optional.of(SaldoCuentaFinanciera.builder().saldo(new BigDecimal("5000.00")).fechaCorte(fechaCorte).build()));
        when(pagoVentaRepository.cobrosPorSedeYMetodo(EstadoVenta.COMPLETADA, fechaCorte, ahora, null)).thenReturn(List.of(
                cobro(centro, MetodoPago.YAPE, "800.00"), cobro(centro, MetodoPago.EFECTIVO, "999.00")));
        when(pagoCxpRepository.pagosPorSede(fechaCorte, ahora, null)).thenReturn(List.of(
                pagoCxp(centro, TipoReferenciaCxP.COMPRA, MetodoPago.TRANSFERENCIA, "300.00")));

        // when
        DisponibleCalculadoDTO d = calculator.disponible(null, ahora);

        // then: 5000 + 800 - 300 (el efectivo no es de esta cuenta)
        SaldoCuentaEstimadoDTO c = d.cuentas().get(0);
        assertEquals(new BigDecimal("500.00"), c.movimientosPosteriores());
        assertEquals(new BigDecimal("5500.00"), c.saldoEstimado());
        assertEquals(3L, c.diasDesdeCorte());
        assertEquals(new BigDecimal("5500.00"), d.total());
        assertTrue(d.advertencias().stream().anyMatch(a -> a.contains("EFECTIVO")), "debe avisar métodos sin cuenta");
    }

    @Test
    void disponible_cuentaSinCorte_noSumaYAvisa() {
        CuentaFinanciera caja = cuenta("Caja fuerte", null, MetodoPago.values());
        when(cuentaRepository.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of(caja));
        when(saldoRepository.findFirstByCuenta_IdOrderByFechaCorteDescCreatedAtDesc(caja.getId())).thenReturn(Optional.empty());

        DisponibleCalculadoDTO d = calculator.disponible(null, LocalDateTime.now());

        assertEquals(new BigDecimal("0.00"), d.total());
        assertNull(d.cuentas().get(0).saldoEstimado());
        assertEquals(1, d.advertencias().size());
        assertTrue(d.advertencias().get(0).contains("no tiene saldo registrado"));
        verifyNoInteractions(pagoVentaRepository);
    }

    @Test
    void disponible_filtroPorSede_excluyeCorporativasYCorteAntiguoAvisa() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 8, 18, 0);
        CuentaFinanciera corporativa = cuenta("BCP", null, MetodoPago.TRANSFERENCIA);
        CuentaFinanciera cajaCentro = cuenta("Caja Centro", centro);   // sin métodos: solo el corte
        when(cuentaRepository.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of(corporativa, cajaCentro));
        when(saldoRepository.findFirstByCuenta_IdOrderByFechaCorteDescCreatedAtDesc(cajaCentro.getId()))
                .thenReturn(Optional.of(SaldoCuentaFinanciera.builder().saldo(new BigDecimal("700.00")).fechaCorte(ahora.minusDays(45)).build()));

        DisponibleCalculadoDTO d = calculator.disponible(centro, ahora);

        assertEquals(1, d.cuentas().size());
        assertEquals(new BigDecimal("700.00"), d.total());
        assertTrue(d.advertencias().stream().anyMatch(a -> a.contains("45 días")));
        assertTrue(d.advertencias().stream().anyMatch(a -> a.contains("corporativas")));
        verifyNoInteractions(pagoVentaRepository);
    }

    // ===================== HELPERS =====================

    private CuentaFinanciera cuenta(String nombre, UUID sedeId, MetodoPago... metodos) {
        return CuentaFinanciera.builder().id(UUID.randomUUID()).nombre(nombre).tipo(TipoCuentaFinanciera.BANCO)
                .sede(sedeId != null ? Sede.builder().id(sedeId).nombre("Sede").build() : null)
                .metodosPago(new HashSet<>(Arrays.asList(metodos))).activa(true).build();
    }

    static MontoPorSedeMetodoProjection cobro(UUID sede, MetodoPago metodo, String total) {
        return new MontoPorSedeMetodoProjection() {
            public UUID getSedeId() { return sede; }
            public MetodoPago getMetodo() { return metodo; }
            public BigDecimal getTotal() { return new BigDecimal(total); }
        };
    }

    static PagoCxpPorSedeProjection pagoCxp(UUID sede, TipoReferenciaCxP tipo, MetodoPago metodo, String total) {
        return new PagoCxpPorSedeProjection() {
            public UUID getSedeId() { return sede; }
            public TipoReferenciaCxP getTipo() { return tipo; }
            public MetodoPago getMetodo() { return metodo; }
            public BigDecimal getTotal() { return new BigDecimal(total); }
        };
    }

    static PagoCxpHistoricoProjection pagoHistorico(UUID sede, TipoReferenciaCxP tipo, String metodo, String total) {
        return new PagoCxpHistoricoProjection() {
            public UUID getSedeId() { return sede; }
            public TipoReferenciaCxP getTipo() { return tipo; }
            public String getMetodo() { return metodo; }
            public BigDecimal getTotal() { return new BigDecimal(total); }
        };
    }

    static ReembolsoPorSedeMetodoProjection reembolso(UUID sede, MetodoReembolso metodo, String total) {
        return new ReembolsoPorSedeMetodoProjection() {
            public UUID getSedeId() { return sede; }
            public MetodoReembolso getMetodo() { return metodo; }
            public BigDecimal getTotal() { return new BigDecimal(total); }
        };
    }

    static RetiroPorSedeOrigenProjection retiro(UUID sede, OrigenFondos origen, String total) {
        return new RetiroPorSedeOrigenProjection() {
            public UUID getSedeId() { return sede; }
            public OrigenFondos getOrigen() { return origen; }
            public BigDecimal getTotal() { return new BigDecimal(total); }
        };
    }
}
