package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.response.UtilidadNetaResponseDTO;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.infrastructure.repository.projection.MontoCantidadProjection;
import com.policalum.erp.infrastructure.repository.projection.TotalesFinancierosProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UtilidadNetaServiceImplTest {

    @Mock VentaRepository ventaRepository;
    @Mock CompraRepository compraRepository;
    @Mock RrhhNominaRepository nominaRepository;
    @Mock FinanzasRetiroSocioRepository retiroRepository;
    @Mock UserRepository userRepository;
    @Mock DevolucionRepository devolucionRepository;
    @Mock KardexRepository kardexRepository;
    @Mock GastoEmpresarialRepository gastoRepository;
    @InjectMocks UtilidadNetaServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    UUID sedeId = UUID.randomUUID();
    User usuario;

    @BeforeEach
    void setUp() {
        usuario = User.builder().id(UUID.randomUUID()).nombre("Miguel").apellido("Dueño").email(EMAIL)
                .rol(Rol.builder().nombre(RolNombre.ROLE_ADMIN).build()).build();
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(usuario));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void calcular_aplicaLaFormulaDeUtilidad() {
        // given: ventas netas 1000, compras netas 400, nóminas 300, retiros 100
        LocalDate desde = LocalDate.of(2026, 10, 1), hasta = LocalDate.of(2026, 10, 31);
        stubTotales("1000.00", "180.00", "1180.00", 12, "400.00", "472.00", 3, "300.00", 2, "100.00");

        // when
        UtilidadNetaResponseDTO r = service.calcular(sedeId, desde, hasta);

        // then
        assertEquals(new BigDecimal("600.00"), r.utilidadBruta());
        assertEquals(new BigDecimal("300.00"), r.gastosOperativos());
        assertEquals(new BigDecimal("300.00"), r.utilidadOperativa());
        assertEquals(new BigDecimal("300.00"), r.utilidadNetaReal());
        assertEquals(new BigDecimal("30.00"), r.margenNetoPorcentaje());
        assertEquals(new BigDecimal("100.00"), r.retirosSocios());
        assertEquals(new BigDecimal("200.00"), r.saldoTrasRetiros());
        assertEquals(12, r.cantidadVentas());
        assertEquals(3, r.cantidadCompras());
        verify(nominaRepository).sumarTotalNeto(EstadoNomina.PAGADA, "2026-10", "2026-10", sedeId);
    }

    @Test
    void calcular_sinVentas_margenCeroYUtilidadNegativa() {
        // given
        stubTotales("0", "0", "0", 0, "200.00", "236.00", 1, "0", 0, "0");

        // when
        UtilidadNetaResponseDTO r = service.calcular(null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        // then
        assertEquals(new BigDecimal("-200.00"), r.utilidadNetaReal());
        assertEquals(new BigDecimal("0.00"), r.margenNetoPorcentaje());
    }

    @Test
    void calcularPorPeriodo_usaElMesCompletoComoRango() {
        // given
        stubTotales("0", "0", "0", 0, "0", "0", 0, "0", 0, "0");

        // when
        UtilidadNetaResponseDTO r = service.calcularPorPeriodo(sedeId, "2026-02");

        // then
        assertEquals(LocalDate.of(2026, 2, 1), r.desde());
        assertEquals(LocalDate.of(2026, 2, 28), r.hasta());
        verify(ventaRepository).totalesPorPeriodo(EstadoVenta.COMPLETADA,
                LocalDateTime.of(2026, 2, 1, 0, 0), LocalDateTime.of(2026, 3, 1, 0, 0), sedeId);
        verify(compraRepository).totalesPorPeriodo(EstadoCompra.RECIBIDA, Moneda.PEN,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), sedeId);
    }

    @Test
    void calcularPorPeriodo_formatoInvalido_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.calcularPorPeriodo(sedeId, "10-2026"));
        assertThrows(IllegalArgumentException.class, () -> service.calcularPorPeriodo(sedeId, null));
    }

    @Test
    void calcular_desdePosteriorAHasta_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calcular(sedeId, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1)));
    }

    @Test
    void calcular_usuarioNoAdmin_lanzaAccessDenied() {
        usuario.setRol(Rol.builder().nombre(RolNombre.ROLE_MULTIFUNCIONAL).build());
        assertThrows(AccessDeniedException.class, () -> service.calcular(sedeId, null, null));
        verifyNoInteractions(ventaRepository, compraRepository, nominaRepository, retiroRepository, devolucionRepository);
    }

    @Test
    void calcular_conComprasOtraMoneda_lasInformaYExcluye() {
        // given
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);

        when(ventaRepository.totalesPorPeriodo(eq(EstadoVenta.COMPLETADA), any(), any(), any()))
            .thenReturn(totales("1000.00", "180.00", "1180.00", 5));
        when(compraRepository.totalesPorPeriodo(eq(EstadoCompra.RECIBIDA), eq(Moneda.PEN), any(), any(), any()))
            .thenReturn(totales("500.00", "0", "500.00", 2));
        when(compraRepository.contarEnOtraMoneda(eq(EstadoCompra.RECIBIDA), eq(Moneda.PEN), any(), any(), any()))
            .thenReturn(3L);  // ← 3 compras en USD
        when(nominaRepository.sumarTotalNeto(any(), anyString(), anyString(), any()))
            .thenReturn(BigDecimal.ZERO);
        when(nominaRepository.contar(any(), anyString(), anyString(), any())).thenReturn(0L);
        when(retiroRepository.sumarMonto(any(), any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);

        // when
        UtilidadNetaResponseDTO r = service.calcular(null, desde, hasta);

        // then
        assertEquals(3L, r.comprasOtraMonedaExcluidas());
        assertEquals(new BigDecimal("500.00"), r.utilidadBruta());  // solo compras PEN
    }

    // ===================== MÓDULO 6: DEVOLUCIONES Y MERMAS =====================

    @Test
    void calcular_devolucionesAprobadasRestanDeLasVentasSinIgv() {
        // given: ventas netas 1000; devoluciones por S/ 118.00 con IGV (= 100.00 netos)
        stubTotales("1000.00", "180.00", "1180.00", 12, "400.00", "472.00", 3, "300.00", 2, "0");
        when(devolucionRepository.totalDevueltoPorPeriodo(eq(sedeId), any(), any())).thenReturn(devueltas("118.00", 2));

        // when
        UtilidadNetaResponseDTO r = service.calcular(sedeId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        // then: 900 - 400 - 300 = 200; margen sobre ventas reales 200/900
        assertEquals(new BigDecimal("118.00"), r.devolucionesTotales());
        assertEquals(new BigDecimal("100.00"), r.devolucionesNetas());
        assertEquals(2, r.cantidadDevoluciones());
        assertEquals(new BigDecimal("900.00"), r.ventasNetasReales());
        assertEquals(new BigDecimal("1000.00"), r.ventasNetas());
        assertEquals(new BigDecimal("500.00"), r.utilidadBruta());
        assertEquals(new BigDecimal("200.00"), r.utilidadNetaReal());
        assertEquals(new BigDecimal("22.22"), r.margenNetoPorcentaje());
    }

    @Test
    void calcular_devolucionesUsanRangoSemiabiertoDelPeriodo() {
        stubTotales("0", "0", "0", 0, "0", "0", 0, "0", 0, "0");

        service.calcular(sedeId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        verify(devolucionRepository).totalDevueltoPorPeriodo(sedeId,
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0));
    }

    @Test
    void calcular_mermasSonInformativasYNoRestan() {
        // given
        stubTotales("1000.00", "180.00", "1180.00", 12, "400.00", "472.00", 3, "300.00", 2, "0");
        when(kardexRepository.valorMermas(eq(sedeId), any(), any())).thenReturn(new BigDecimal("75.5"));

        // when
        UtilidadNetaResponseDTO r = service.calcular(sedeId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        // then
        assertEquals(new BigDecimal("75.50"), r.valorMermas());
        assertEquals(new BigDecimal("300.00"), r.utilidadNetaReal());
    }

    @Test
    void calcular_todoDevuelto_margenCeroSinDividirPorCero() {
        stubTotales("100.00", "18.00", "118.00", 1, "0", "0", 0, "0", 0, "0");
        when(devolucionRepository.totalDevueltoPorPeriodo(any(), any(), any())).thenReturn(devueltas("118.00", 1));

        UtilidadNetaResponseDTO r = service.calcular(null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertEquals(0, r.ventasNetasReales().signum());
        assertEquals(new BigDecimal("0.00"), r.margenNetoPorcentaje());
    }

    // ===================== HELPERS =====================

    private MontoCantidadProjection devueltas(String total, long cantidad) {
        return new MontoCantidadProjection() {
            public BigDecimal getTotal() { return new BigDecimal(total); }
            public Long getCantidad() { return cantidad; }
        };
    }

    private void stubTotales(String ventaNeto, String ventaIgv, String ventaTotal, long ventas,
                             String compraNeto, String compraTotal, long compras,
                             String nominas, long cantNominas, String retiros) {
        when(ventaRepository.totalesPorPeriodo(eq(EstadoVenta.COMPLETADA), any(), any(), any()))
                .thenReturn(totales(ventaNeto, ventaIgv, ventaTotal, ventas));
        when(compraRepository.totalesPorPeriodo(eq(EstadoCompra.RECIBIDA), eq(Moneda.PEN), any(), any(), any()))
                .thenReturn(totales(compraNeto, "0", compraTotal, compras));
        when(compraRepository.contarEnOtraMoneda(any(), any(), any(), any(), any())).thenReturn(0L);
        when(nominaRepository.sumarTotalNeto(eq(EstadoNomina.PAGADA), anyString(), anyString(), any()))
                .thenReturn(new BigDecimal(nominas));
        when(nominaRepository.contar(eq(EstadoNomina.PAGADA), anyString(), anyString(), any())).thenReturn(cantNominas);
        when(retiroRepository.sumarMonto(eq(EstadoRetiro.REGISTRADO), any(), any(), any()))
                .thenReturn(new BigDecimal(retiros));
    }

    private TotalesFinancierosProjection totales(String neto, String igv, String total, long cantidad) {
        return new TotalesFinancierosProjection() {
            public BigDecimal getNeto() { return new BigDecimal(neto); }
            public BigDecimal getIgv() { return new BigDecimal(igv); }
            public BigDecimal getTotal() { return new BigDecimal(total); }
            public Long getCantidad() { return cantidad; }
        };
    }
}
