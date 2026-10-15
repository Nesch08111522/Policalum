package com.policalum.erp.application.service.serviceImpl;

import static com.policalum.erp.application.service.serviceImpl.FinanzasTestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.response.*;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.application.service.UtilidadNetaService;
import com.policalum.erp.application.service.serviceImpl.MovimientosCaja.Concepto;
import com.policalum.erp.application.service.serviceImpl.MovimientosCaja.Movimiento;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.infrastructure.repository.projection.CxpPendienteProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class FlujoCajaServiceImplTest {

    @Mock MovimientosCajaCalculator calculator;
    @Mock UtilidadNetaService utilidadNetaService;
    @Mock FinanzasCuentaPorPagarRepository cxpRepository;
    @Mock RrhhNominaRepository nominaRepository;
    @Mock ReembolsoRepository reembolsoRepository;
    @Mock CotizacionRepository cotizacionRepository;
    @Mock PreformaVentaRepository preformaRepository;
    @Mock SedeRepository sedeRepository;
    @Mock UserRepository userRepository;
    @Mock ConfiguracionService configuracionService;
    @InjectMocks FlujoCajaServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    User admin;
    Sede centro, norte, cerrada;

    @BeforeEach
    void setUp() {
        lenient().when(configuracionService.finanzas()).thenReturn(ParametrosFinanzasDTO.DEFAULTS);
        admin = User.builder().id(UUID.randomUUID()).email(EMAIL).rol(Rol.builder().nombre(RolNombre.ROLE_ADMIN).build()).build();
        centro = Sede.builder().id(UUID.randomUUID()).nombre("Centro").activa(true).build();
        norte = Sede.builder().id(UUID.randomUUID()).nombre("Norte").activa(true).build();
        cerrada = Sede.builder().id(UUID.randomUUID()).nombre("Antigua").activa(false).build();
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(admin));
        lenient().when(sedeRepository.findAll()).thenReturn(List.of(norte, cerrada, centro));
        lenient().when(sedeRepository.findById(centro.getId())).thenReturn(Optional.of(centro));
        // proyección: sin documentos ni cuentas salvo que el test diga otra cosa
        lenient().when(cxpRepository.pendientesPorVencimiento(any())).thenReturn(List.of());
        lenient().when(nominaRepository.porPagarPorSede(any(), any())).thenReturn(List.of());
        lenient().when(reembolsoRepository.pendientesPorSede(any())).thenReturn(List.of());
        lenient().when(calculator.disponible(any(), any())).thenReturn(new DisponibleCalculadoDTO(bd("0.00"), List.of(), List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== FLUJO REAL =====================

    @Test
    void flujoReal_consolidadoYDesgloseOrdenadoSinSedesInactivasVacias() {
        // given
        when(calculator.movimientos(any(), any(), isNull())).thenReturn(new MovimientosCaja(List.of(
                mov(centro, Concepto.COBRO_VENTA, MetodoPago.EFECTIVO, "800.00"),
                mov(norte, Concepto.COBRO_VENTA, MetodoPago.YAPE, "200.00"),
                mov(centro, Concepto.PAGO_PROVEEDOR, MetodoPago.TRANSFERENCIA, "300.00"))));

        // when
        FlujoCajaRealResponseDTO r = service.flujoReal(null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        // then
        assertEquals(bd("700.00"), r.consolidado().flujoOperativo().total());
        assertEquals(List.of("Centro", "Norte"), r.porSede().stream().map(FlujoCajaSedeDTO::sedeNombre).toList());
        assertEquals(bd("500.00"), r.porSede().get(0).resumen().flujoOperativo().total());
        assertEquals(bd("200.00"), r.porSede().get(1).resumen().flujoOperativo().total());
        assertTrue(r.advertencias().isEmpty());
    }

    @Test
    void flujoReal_conSede_usaRangoSemiabiertoYSoloEsaSede() {
        when(calculator.movimientos(any(), any(), eq(centro.getId()))).thenReturn(new MovimientosCaja(List.of()));

        FlujoCajaRealResponseDTO r = service.flujoReal(centro.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        verify(calculator).movimientos(LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0), centro.getId());
        assertEquals(1, r.porSede().size());
        assertEquals(bd("0.00"), r.consolidado().flujoNeto().total());
    }

    @Test
    void flujoReal_egresosSinMetodo_advierte() {
        when(calculator.movimientos(any(), any(), any())).thenReturn(new MovimientosCaja(List.of(
                mov(centro, Concepto.PAGO_NOMINA, null, "1200.00"))));

        FlujoCajaRealResponseDTO r = service.flujoReal(null, null, null);

        assertEquals(1, r.advertencias().size());
        assertTrue(r.advertencias().get(0).contains("1200.00"));
        assertEquals(LocalDate.now().withDayOfMonth(1), r.desde());
    }

    @Test
    void flujoReal_desdePosteriorAHasta_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.flujoReal(null, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1)));
        verifyNoInteractions(calculator);
    }

    @Test
    void flujoReal_sedeInexistente_lanzaNotFound() {
        UUID otra = UUID.randomUUID();
        when(sedeRepository.findById(otra)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.flujoReal(otra, null, null));
    }

    @Test
    void flujoReal_noAdmin_lanzaAccessDenied() {
        admin.setRol(Rol.builder().nombre(RolNombre.ROLE_MULTIFUNCIONAL).build());
        assertThrows(AccessDeniedException.class, () -> service.flujoReal(null, null, null));
        verifyNoInteractions(calculator);
    }

    // ===================== DEVENGADO =====================

    @Test
    void devengado_consolidadoMasCadaSedeActivaOConMovimiento() {
        LocalDate d = LocalDate.of(2026, 10, 1), h = LocalDate.of(2026, 10, 31);
        when(utilidadNetaService.calcular(null, d, h)).thenReturn(utilidad("1000", "0", "400", "300", "300", "30", "0", 5, "1180"));
        when(utilidadNetaService.calcular(centro.getId(), d, h)).thenReturn(utilidad("1000", "0", "400", "300", "300", "30", "0", 5, "1180"));
        when(utilidadNetaService.calcular(norte.getId(), d, h)).thenReturn(utilidadCero());
        when(utilidadNetaService.calcular(cerrada.getId(), d, h)).thenReturn(utilidadCero());

        FlujoDevengadoResponseDTO r = service.devengado(null, d, h);

        assertEquals(bd("300"), r.consolidado().utilidadNetaReal());
        assertEquals(List.of("Centro", "Norte"), r.porSede().stream().map(UtilidadSedeDTO::sedeNombre).toList());
    }

    // ===================== PROYECCIÓN =====================

    @Test
    void proyeccion_mesesFueraDeRango_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.proyeccion(null, 0));
        assertThrows(IllegalArgumentException.class, () -> service.proyeccion(null, 13));
        verifyNoInteractions(calculator);
    }

    @Test
    void proyeccion_respetaMaximoConfigurado() {
        // Módulo 14: máximo 6 meses → 7 se rechaza aunque el valor de fábrica (12) lo permitiría
        when(configuracionService.finanzas()).thenReturn(new ParametrosFinanzasDTO(3, 3, 6));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.proyeccion(null, 7));
        assertTrue(ex.getMessage().contains("1 a 6"));
        verifyNoInteractions(calculator);
    }

    @Test
    void proyeccion_usaElMayorEntreTendenciaYComprometidoYAcumulaSaldo() {
        // given: 3 meses de historia, cada uno con cobros 3000, proveedores 1000, nómina 800, retiros 200
        when(calculator.movimientos(any(), any(), isNull())).thenReturn(new MovimientosCaja(List.of(
                mov(centro, Concepto.COBRO_VENTA, MetodoPago.EFECTIVO, "3000.00"),
                mov(centro, Concepto.PAGO_PROVEEDOR, MetodoPago.TRANSFERENCIA, "1000.00"),
                mov(centro, Concepto.PAGO_NOMINA, MetodoPago.TRANSFERENCIA, "800.00"),
                mov(centro, Concepto.RETIRO_SOCIO, MetodoPago.EFECTIVO, "200.00"))));
        YearMonth actual = YearMonth.now();
        // CxP: S/ 2500 a proveedor vence el mes siguiente (> tendencia 1000); S/ 50 de servicio en 2 meses
        when(cxpRepository.pendientesPorVencimiento(null)).thenReturn(List.of(
                pendiente(TipoReferenciaCxP.COMPRA, actual.plusMonths(1).atDay(10), "2500.00"),
                pendiente(TipoReferenciaCxP.SERVICIO, actual.plusMonths(2).atDay(5), "50.00"),
                pendiente(TipoReferenciaCxP.COMPRA, actual.plusMonths(8).atDay(5), "999.00")));
        when(calculator.disponible(isNull(), any())).thenReturn(new DisponibleCalculadoDTO(bd("10000.00"), List.of(), List.of()));
        when(cotizacionRepository.sumarVigentes(any(), any(), isNull())).thenReturn(bd("700.00"));
        when(preformaRepository.sumarAbiertasVigentes(isNull(), any())).thenReturn(bd("300.00"));

        // when
        ProyeccionFlujoResponseDTO r = service.proyeccion(null, 3);

        // then
        assertEquals(3, r.mesesHistoricosUsados());
        assertEquals(bd("3000.00"), r.promedioIngresos());
        assertEquals(bd("1800.00"), r.promedioEgresos());
        assertEquals(bd("1000.00"), r.ingresosPotenciales());
        MesProyectadoDTO m1 = r.meses().get(1);
        assertFalse(m1.parcial());
        assertEquals(bd("2500.00"), m1.pagosProveedores().estimado());
        assertEquals(bd("1000.00"), m1.pagosProveedores().tendencia());
        assertEquals(bd("-300.00"), m1.flujoOperativo());   // 3000 - 2500 - 800
        assertEquals(bd("-500.00"), m1.flujoNeto());        // - 200 retiros
        MesProyectadoDTO m2 = r.meses().get(2);
        assertEquals(bd("1000.00"), m2.pagosProveedores().estimado());
        assertEquals(bd("50.00"), m2.otrosPagos().estimado());
        assertEquals(bd("950.00"), m2.flujoNeto());          // 3000 - 1000 - 800 - 50 - 200
        // saldo: 10000 + mes0 + (-500) + 950
        MesProyectadoDTO m0 = r.meses().get(0);
        assertTrue(m0.parcial());
        assertEquals(bd("10000.00").add(m0.flujoNeto()).add(bd("450.00")), m2.saldoFinalEstimado());
        assertTrue(r.advertencias().stream().anyMatch(a -> a.contains("999.00") && a.contains("horizonte")));
    }

    @Test
    void proyeccion_mesActualProrrateaTendenciaPeroNoLoComprometido() {
        when(calculator.movimientos(any(), any(), isNull())).thenReturn(new MovimientosCaja(List.of(
                mov(centro, Concepto.COBRO_VENTA, MetodoPago.EFECTIVO, "3100.00"))));
        // nóminas por pagar: comprometidas en el mes en curso aunque ya se vaya acabando
        when(nominaRepository.porPagarPorSede(any(), isNull())).thenReturn(List.of(monto(centro.getId(), "1500.00")));
        // CxP vencida hace meses → también al mes en curso
        when(cxpRepository.pendientesPorVencimiento(null)).thenReturn(List.of(
                pendiente(TipoReferenciaCxP.COMPRA, LocalDate.now().minusMonths(3), "400.00")));

        ProyeccionFlujoResponseDTO r = service.proyeccion(null, 1);

        MesProyectadoDTO m0 = r.meses().get(0);
        LocalDate hoy = LocalDate.now();
        BigDecimal fraccion = BigDecimal.valueOf(hoy.lengthOfMonth() - hoy.getDayOfMonth() + 1)
                .divide(BigDecimal.valueOf(hoy.lengthOfMonth()), 4, RoundingMode.HALF_UP);
        assertEquals(fraccion, m0.fraccionDelMes());
        assertEquals(bd("3100.00").multiply(fraccion).setScale(2, RoundingMode.HALF_UP), m0.ingresosEstimados());
        assertEquals(bd("1500.00"), m0.pagosNominas().estimado());
        assertEquals(bd("400.00"), m0.pagosProveedores().estimado());
    }

    @Test
    void proyeccion_sinHistoria_adviertePocaConfianzaYSaldoNegativo() {
        when(calculator.movimientos(any(), any(), isNull())).thenReturn(new MovimientosCaja(List.of()));
        when(reembolsoRepository.pendientesPorSede(isNull())).thenReturn(List.of(monto(centro.getId(), "80.00")));
        when(cotizacionRepository.sumarVigentes(any(), any(), any())).thenReturn(null);
        when(preformaRepository.sumarAbiertasVigentes(any(), any())).thenReturn(null);

        ProyeccionFlujoResponseDTO r = service.proyeccion(null, null);

        assertEquals(3, r.meses().size());
        assertEquals(0, r.mesesHistoricosUsados());
        assertEquals(bd("-80.00"), r.meses().get(0).saldoFinalEstimado());
        assertTrue(r.advertencias().stream().anyMatch(a -> a.contains("0 de 3 meses")));
        assertTrue(r.advertencias().stream().anyMatch(a -> a.contains("negativo")));
        assertEquals(bd("0.00"), r.ingresosPotenciales());
    }

    @Test
    void indiceMes_vencidasYSinFechaVanAlMesActual() {
        YearMonth oct = YearMonth.of(2026, 10);
        assertEquals(0, FlujoCajaServiceImpl.indiceMes(oct, null));
        assertEquals(0, FlujoCajaServiceImpl.indiceMes(oct, LocalDate.of(2026, 1, 1)));
        assertEquals(0, FlujoCajaServiceImpl.indiceMes(oct, LocalDate.of(2026, 10, 31)));
        assertEquals(2, FlujoCajaServiceImpl.indiceMes(oct, LocalDate.of(2026, 12, 1)));
    }

    // ===================== HELPERS =====================

    private Movimiento mov(Sede sede, Concepto concepto, MetodoPago metodo, String monto) {
        return new Movimiento(sede.getId(), concepto, metodo, bd(monto));
    }

    private CxpPendienteProjection pendiente(TipoReferenciaCxP tipo, LocalDate vence, String saldo) {
        return new CxpPendienteProjection() {
            public TipoReferenciaCxP getTipo() { return tipo; }
            public LocalDate getFechaVencimiento() { return vence; }
            public BigDecimal getSaldo() { return bd(saldo); }
        };
    }
}
