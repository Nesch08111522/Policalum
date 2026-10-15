package com.policalum.erp.application.service.serviceImpl;

import static com.policalum.erp.application.service.serviceImpl.FinanzasTestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.response.*;
import com.policalum.erp.application.service.UtilidadNetaService;
import com.policalum.erp.application.service.serviceImpl.MovimientosCaja.Concepto;
import com.policalum.erp.application.service.serviceImpl.MovimientosCaja.Movimiento;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.infrastructure.repository.projection.PreformasPorEstadoProjection;
import com.policalum.erp.infrastructure.repository.projection.RetiroPorAdminProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DashboardFinancieroServiceImplTest {

    @Mock MovimientosCajaCalculator calculator;
    @Mock UtilidadNetaService utilidadNetaService;
    @Mock StockRepository stockRepository;
    @Mock FinanzasCuentaPorPagarRepository cxpRepository;
    @Mock RrhhNominaRepository nominaRepository;
    @Mock ReembolsoRepository reembolsoRepository;
    @Mock FinanzasRetiroSocioRepository retiroRepository;
    @Mock PreformaVentaRepository preformaRepository;
    @Mock SedeRepository sedeRepository;
    @Mock UserRepository userRepository;
    @InjectMocks DashboardFinancieroServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    User admin;
    Sede centro, norte;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(UUID.randomUUID()).email(EMAIL).rol(Rol.builder().nombre(RolNombre.ROLE_ADMIN).build()).build();
        centro = Sede.builder().id(UUID.randomUUID()).nombre("Centro").activa(true).build();
        norte = Sede.builder().id(UUID.randomUUID()).nombre("Norte").activa(true).build();
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(admin));
        lenient().when(sedeRepository.findAll()).thenReturn(List.of(norte, centro));
        lenient().when(sedeRepository.findById(centro.getId())).thenReturn(Optional.of(centro));
        lenient().when(cxpRepository.saldoVencidoPorSede(any(), any())).thenReturn(List.of());
        lenient().when(preformaRepository.totalesPorEstado(any(), any(), any())).thenReturn(List.of());
        lenient().when(retiroRepository.totalesPorAdmin(any(), any(), any(), any())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== PATRIMONIO =====================

    @Test
    void patrimonio_activosMenosPasivosConDesglosePorSedeYCorporativo() {
        // given: BCP corporativa 3000 + Caja Centro 2000; inventario Centro 8000;
        //        CxP 1500 (500 vencida), nóminas por pagar 1000, reembolsos 200
        when(calculator.disponible(isNull(), any())).thenReturn(new DisponibleCalculadoDTO(bd("5000.00"), List.of(
                saldo("BCP", null, "3000.00"), saldo("Caja Centro", centro.getId(), "2000.00")), List.of()));
        when(stockRepository.valorizadoPorSede(null)).thenReturn(List.of(monto(centro.getId(), "8000.00")));
        when(stockRepository.contarSinCosto(null)).thenReturn(0L);
        when(cxpRepository.saldoPendientePorSede(null)).thenReturn(List.of(monto(centro.getId(), "1500.00")));
        when(cxpRepository.saldoVencidoPorSede(isNull(), any())).thenReturn(List.of(monto(centro.getId(), "500.00")));
        when(nominaRepository.porPagarPorSede(List.of(EstadoNomina.PENDIENTE, EstadoNomina.APROBADA), null))
                .thenReturn(List.of(monto(centro.getId(), "600.00"), monto(norte.getId(), "400.00")));
        when(reembolsoRepository.pendientesPorSede(null)).thenReturn(List.of(monto(norte.getId(), "200.00")));
        stubSeparacion("12000.00", "4000.00", "1000.00");

        // when
        PatrimonioResponseDTO r = service.patrimonio(null);

        // then
        assertEquals(bd("13000.00"), r.totalActivos());
        assertEquals(bd("2700.00"), r.totalPasivos());
        assertEquals(bd("10300.00"), r.patrimonioNeto());
        assertEquals(bd("500.00"), r.cuentasPorPagarVencidas());
        Map<String, PatrimonioSedeDTO> porSede = new HashMap<>();
        r.porSede().forEach(p -> porSede.put(p.sedeNombre(), p));
        assertEquals(bd("10000.00"), porSede.get("Centro").totalActivos());
        assertEquals(bd("2100.00"), porSede.get("Centro").totalPasivos());
        assertEquals(bd("-600.00"), porSede.get("Norte").patrimonioNeto());
        assertTrue(porSede.containsKey("Corporativo (cuentas sin sede)"));
        assertEquals(bd("3000.00"), porSede.get("Corporativo (cuentas sin sede)").disponible());
        // separación dueño / empresa
        assertEquals(bd("8000.00"), r.separacion().utilidadRetenidaAnio());
        assertEquals(bd("1000.00"), r.separacion().retirosSociosMesActual());
        assertEquals(LocalDate.now().getYear(), r.separacion().anio());
    }

    @Test
    void patrimonio_productosSinCostoYRetirosMayoresALaUtilidad() {
        when(calculator.disponible(isNull(), any())).thenReturn(new DisponibleCalculadoDTO(bd("0.00"), List.of(), List.of("aviso cuentas")));
        when(stockRepository.valorizadoPorSede(null)).thenReturn(List.of());
        when(stockRepository.contarSinCosto(null)).thenReturn(4L);
        when(cxpRepository.saldoPendientePorSede(null)).thenReturn(List.of());
        when(nominaRepository.porPagarPorSede(any(), isNull())).thenReturn(List.of());
        when(reembolsoRepository.pendientesPorSede(null)).thenReturn(List.of());
        stubSeparacion("1000.00", "2500.00", "0");
        when(retiroRepository.totalesPorAdmin(eq(EstadoRetiro.REGISTRADO), isNull(), any(), any())).thenReturn(List.of(socio("Miguel", "Dueño", 3, "2500.00")));

        PatrimonioResponseDTO r = service.patrimonio(null);

        assertEquals(List.of("aviso cuentas", "4 productos con stock no tienen precio de compra: no suman al inventario"), r.advertencias());
        assertEquals(bd("-1500.00"), r.separacion().utilidadRetenidaAnio());
        assertEquals("Miguel Dueño", r.separacion().retirosPorSocio().get(0).socioNombre());
        assertEquals(bd("0.00"), r.patrimonioNeto());
    }

    // ===================== KPIs =====================

    @Test
    void kpis_calculaIndicadoresYVariacionContraPeriodoAnterior() {
        // given: octubre vs septiembre (mismos 30 días previos)
        LocalDate d = LocalDate.of(2026, 10, 1), h = LocalDate.of(2026, 10, 30);
        when(calculator.movimientos(any(), any(), eq(centro.getId()))).thenReturn(new MovimientosCaja(List.of(
                new Movimiento(centro.getId(), Concepto.COBRO_VENTA, MetodoPago.EFECTIVO, bd("1180.00")),
                new Movimiento(centro.getId(), Concepto.RETIRO_SOCIO, MetodoPago.EFECTIVO, bd("100.00")))));
        when(utilidadNetaService.calcular(centro.getId(), d, h))
                .thenReturn(utilidad("1000.00", "50.00", "400.00", "300.00", "250.00", "26.32", "100.00", 4, "1180.00"));
        when(utilidadNetaService.calcular(centro.getId(), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(utilidad("1000.00", "0", "400.00", "300.00", "-50.00", "0", "0", 2, "1180.00"));
        when(preformaRepository.totalesPorEstado(eq(centro.getId()), any(), any())).thenReturn(List.of(
                preformas(EstadoPreforma.CONFIRMADA, 3, "900.00"), preformas(EstadoPreforma.EXPIRADA, 1, "120.00"),
                preformas(EstadoPreforma.ABIERTA, 5, "999.00")));

        // when
        KpisFinancierosDTO k = service.kpis(centro.getId(), d, h).consolidado();

        // then
        assertEquals(bd("950.00"), k.ventasNetasReales());
        assertEquals(bd("295.00"), k.ticketPromedio());            // 1180 / 4
        assertEquals(bd("-5.00"), k.variacionVentasPorcentaje());  // 950 vs 1000
        assertEquals(bd("600.00"), k.variacionUtilidadPorcentaje()); // 250 vs -50 → (300 / 50)
        assertEquals(bd("5.00"), k.tasaDevolucionPorcentaje());    // 50 / 1000
        assertEquals(bd("40.00"), k.retirosSobreUtilidadPorcentaje());
        assertEquals(bd("1180.00"), k.flujoOperativo());
        assertEquals(bd("1080.00"), k.flujoNeto());
        assertEquals(bd("75.00"), k.conversionPreformasPorcentaje()); // 3 / (3+1); las ABIERTAS no cuentan
        assertEquals(bd("120.00"), k.montoPreformasPerdido());
    }

    @Test
    void kpis_sinPeriodoAnteriorNiUtilidad_devuelveNullsEnVezDeDividirPorCero() {
        when(calculator.movimientos(any(), any(), isNull())).thenReturn(new MovimientosCaja(List.of()));
        when(utilidadNetaService.calcular(any(), any(), any())).thenReturn(utilidadCero());

        KpisFinancierosResponseDTO r = service.kpis(null, null, null);

        assertNull(r.consolidado().variacionVentasPorcentaje());
        assertNull(r.consolidado().retirosSobreUtilidadPorcentaje());
        assertEquals(bd("0.00"), r.consolidado().ticketPromedio());
        assertEquals(List.of("Centro", "Norte"), r.porSede().stream().map(KpisSedeDTO::sedeNombre).toList());
    }

    @Test
    void variacion_usaValorAbsolutoDeLaBase() {
        assertEquals(bd("-10.00"), DashboardFinancieroServiceImpl.variacion(bd("900"), bd("1000")));
        assertEquals(bd("50.00"), DashboardFinancieroServiceImpl.variacion(bd("-50"), bd("-100")));
        assertNull(DashboardFinancieroServiceImpl.variacion(bd("5"), BigDecimal.ZERO));
    }

    // ===================== EVOLUCIÓN =====================

    @Test
    void evolucion_serieDelMasAntiguoAlActualYMesEnCursoHastaHoy() {
        when(utilidadNetaService.calcular(any(), any(), any())).thenReturn(utilidad("100", "0", "40", "0", "60", "60", "0", 1, "118"));
        when(calculator.movimientos(any(), any(), isNull())).thenReturn(new MovimientosCaja(List.of()));

        EvolucionMensualResponseDTO r = service.evolucionMensual(null, 3);

        YearMonth actual = YearMonth.now();
        assertEquals(List.of(actual.minusMonths(2).toString(), actual.minusMonths(1).toString(), actual.toString()),
                r.serie().stream().map(MesEvolucionDTO::periodo).toList());
        assertTrue(r.serie().get(2).parcial());
        assertFalse(r.serie().get(0).parcial());
        verify(utilidadNetaService).calcular(null, actual.atDay(1), LocalDate.now());
        verify(utilidadNetaService).calcular(null, actual.minusMonths(1).atDay(1), actual.minusMonths(1).atEndOfMonth());
        assertEquals(bd("60"), r.serie().get(0).utilidadNeta());
    }

    @Test
    void evolucion_fueraDeRango_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.evolucionMensual(null, 25));
        assertThrows(IllegalArgumentException.class, () -> service.evolucionMensual(null, 0));
        verifyNoInteractions(utilidadNetaService);
    }

    // ===================== HELPERS =====================

    private void stubSeparacion(String utilidadAnio, String retirosAnio, String retirosMes) {
        LocalDate hoy = LocalDate.now();
        when(utilidadNetaService.calcular(isNull(), eq(hoy.withDayOfYear(1)), eq(hoy)))
                .thenReturn(utilidad("0", "0", "0", "0", utilidadAnio, "0", "0", 0, "0"));
        when(retiroRepository.sumarMonto(EstadoRetiro.REGISTRADO, null, hoy.withDayOfYear(1).atStartOfDay(), hoy.plusDays(1).atStartOfDay()))
                .thenReturn(bd(retirosAnio));
        lenient().when(retiroRepository.sumarMonto(EstadoRetiro.REGISTRADO, null, hoy.withDayOfMonth(1).atStartOfDay(), hoy.plusDays(1).atStartOfDay()))
                .thenReturn(bd(retirosMes));
    }

    private SaldoCuentaEstimadoDTO saldo(String nombre, UUID sedeId, String estimado) {
        return new SaldoCuentaEstimadoDTO(UUID.randomUUID(), nombre, TipoCuentaFinanciera.BANCO, sedeId, null, List.of(),
                bd(estimado), null, BigDecimal.ZERO, bd(estimado), 1L);
    }

    private PreformasPorEstadoProjection preformas(EstadoPreforma estado, long n, String monto) {
        return new PreformasPorEstadoProjection() {
            public EstadoPreforma getEstado() { return estado; }
            public Long getCantidad() { return n; }
            public BigDecimal getMonto() { return bd(monto); }
        };
    }

    private RetiroPorAdminProjection socio(String nombre, String apellido, long cantidad, String total) {
        return new RetiroPorAdminProjection() {
            public UUID getAdminId() { return UUID.randomUUID(); }
            public String getNombre() { return nombre; }
            public String getApellido() { return apellido; }
            public Long getCantidad() { return cantidad; }
            public BigDecimal getTotal() { return bd(total); }
        };
    }
}
