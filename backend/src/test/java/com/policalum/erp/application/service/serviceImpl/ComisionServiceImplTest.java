package com.policalum.erp.application.service.serviceImpl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.CalcularComisionesRequestDTO;
import com.policalum.erp.application.dto.response.CalculoComisionesResponseDTO;
import com.policalum.erp.application.mapper.ComisionMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.infrastructure.repository.projection.VentasPorVendedorProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ComisionServiceImplTest {

    @Mock RrhhComisionRepository comisionRepository;
    @Mock RrhhLegajoDigitalRepository legajoRepository;
    @Mock VentaRepository ventaRepository;
    @Mock UserRepository userRepository;
    @Mock ComisionMapper mapper;
    @Mock AuditService auditService;
    @InjectMocks ComisionServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    private static final String PERIODO = "2026-09";
    UUID sedeId = UUID.randomUUID();
    UUID comisionId = UUID.randomUUID();
    Sede sede;
    User actual;
    User vendedor;

    @BeforeEach
    void setUp() {
        sede = Sede.builder().id(sedeId).nombre("Trujillo Centro").build();
        actual = usuario("Miguel", RolNombre.ROLE_ADMIN);
        vendedor = usuario("Ana", RolNombre.ROLE_MULTIFUNCIONAL);

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(actual));
        lenient().when(comisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void calcular_creaComisionComoPorcentajeDeVentasNetas() {
        // given: 3% de 10 000.00 = 300.00
        when(ventaRepository.totalesPorVendedor(eq(EstadoVenta.COMPLETADA), any(), any()))
                .thenReturn(List.of(ventas(vendedor.getId(), "10000.00", 25)));
        when(legajoRepository.findActivos(sedeId)).thenReturn(List.of(legajo("3.00")));
        when(comisionRepository.findByUser_IdAndPeriodo(vendedor.getId(), PERIODO)).thenReturn(Optional.empty());
        ArgumentCaptor<RrhhComision> captor = ArgumentCaptor.forClass(RrhhComision.class);

        // when
        CalculoComisionesResponseDTO r = service.calcular(new CalcularComisionesRequestDTO(PERIODO, sedeId));

        // then
        verify(comisionRepository).save(captor.capture());
        RrhhComision c = captor.getValue();
        assertEquals(new BigDecimal("300.00"), c.getMontoComision());
        assertEquals(new BigDecimal("10000.00"), c.getTotalVentasNetas());
        assertEquals(25L, c.getCantidadVentas());
        assertEquals(EstadoComision.CALCULADA, c.getEstado());
        assertSame(sede, c.getSede());
        assertEquals(1, r.nuevas());
        assertEquals(new BigDecimal("300.00"), r.totalComisiones());
        verify(auditService).registrar(eq("rrhh_comisiones"), any(), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void calcular_usaRangoDelMesCompleto() {
        // given
        when(ventaRepository.totalesPorVendedor(any(), any(), any())).thenReturn(List.of());
        when(legajoRepository.findActivos(null)).thenReturn(List.of());

        // when
        service.calcular(new CalcularComisionesRequestDTO(PERIODO, null));

        // then
        verify(ventaRepository).totalesPorVendedor(EstadoVenta.COMPLETADA,
                LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0));
    }

    @Test
    void calcular_comisionCalculada_seRecalcula() {
        // given: ya existía con 100.00, ahora las ventas dan 5% de 4 000 = 200.00
        RrhhComision existente = comision(EstadoComision.CALCULADA, "100.00");
        when(ventaRepository.totalesPorVendedor(any(), any(), any())).thenReturn(List.of(ventas(vendedor.getId(), "4000.00", 8)));
        when(legajoRepository.findActivos(sedeId)).thenReturn(List.of(legajo("5.00")));
        when(comisionRepository.findByUser_IdAndPeriodo(vendedor.getId(), PERIODO)).thenReturn(Optional.of(existente));

        // when
        CalculoComisionesResponseDTO r = service.calcular(new CalcularComisionesRequestDTO(PERIODO, sedeId));

        // then
        assertEquals(new BigDecimal("200.00"), existente.getMontoComision());
        assertEquals(1, r.actualizadas());
        assertEquals(0, r.nuevas());
        verify(auditService).registrar(eq("rrhh_comisiones"), eq(comisionId), eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void calcular_comisionAprobada_noSeTocaYSeAdvierte() {
        // given
        RrhhComision aprobada = comision(EstadoComision.APROBADA, "100.00");
        when(ventaRepository.totalesPorVendedor(any(), any(), any())).thenReturn(List.of(ventas(vendedor.getId(), "9000.00", 8)));
        when(legajoRepository.findActivos(sedeId)).thenReturn(List.of(legajo("3.00")));
        when(comisionRepository.findByUser_IdAndPeriodo(vendedor.getId(), PERIODO)).thenReturn(Optional.of(aprobada));

        // when
        CalculoComisionesResponseDTO r = service.calcular(new CalcularComisionesRequestDTO(PERIODO, sedeId));

        // then
        assertEquals(new BigDecimal("100.00"), aprobada.getMontoComision());
        assertEquals(1, r.omitidas());
        assertTrue(r.advertencias().get(0).contains("APROBADA"));
        verify(comisionRepository, never()).save(any());
    }

    @Test
    void calcular_legajoSinPorcentaje_noGeneraComision() {
        when(ventaRepository.totalesPorVendedor(any(), any(), any())).thenReturn(List.of(ventas(vendedor.getId(), "9000.00", 8)));
        when(legajoRepository.findActivos(sedeId)).thenReturn(List.of(legajo("0.00")));

        CalculoComisionesResponseDTO r = service.calcular(new CalcularComisionesRequestDTO(PERIODO, sedeId));

        assertEquals(0, r.nuevas());
        verify(comisionRepository, never()).save(any());
    }

    @Test
    void calcular_vendedorSinVentas_comisionEnCero() {
        when(ventaRepository.totalesPorVendedor(any(), any(), any())).thenReturn(List.of());
        when(legajoRepository.findActivos(sedeId)).thenReturn(List.of(legajo("3.00")));
        when(comisionRepository.findByUser_IdAndPeriodo(vendedor.getId(), PERIODO)).thenReturn(Optional.empty());
        ArgumentCaptor<RrhhComision> captor = ArgumentCaptor.forClass(RrhhComision.class);

        service.calcular(new CalcularComisionesRequestDTO(PERIODO, sedeId));

        verify(comisionRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getMontoComision().signum());
        assertEquals(0L, captor.getValue().getCantidadVentas());
    }

    @Test
    void calcular_ventasDeUsuarioSinLegajo_seAdvierte() {
        when(ventaRepository.totalesPorVendedor(any(), any(), any())).thenReturn(List.of(ventas(UUID.randomUUID(), "500.00", 2)));
        when(legajoRepository.findActivos(null)).thenReturn(List.of());

        CalculoComisionesResponseDTO r = service.calcular(new CalcularComisionesRequestDTO(PERIODO, null));

        assertEquals(1, r.advertencias().size());
        assertTrue(r.advertencias().get(0).contains("no tienen legajo activo"));
    }

    @Test
    void calcular_periodoFuturo_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.calcular(
                new CalcularComisionesRequestDTO(YearMonth.now().plusMonths(1).toString(), null)));
        verifyNoInteractions(ventaRepository);
    }

    @Test
    void aprobar_comisionCalculada_pasaAAprobada() {
        RrhhComision c = comision(EstadoComision.CALCULADA, "300.00");
        when(comisionRepository.findByIdParaActualizar(comisionId)).thenReturn(Optional.of(c));

        service.aprobar(comisionId);

        assertEquals(EstadoComision.APROBADA, c.getEstado());
        assertSame(actual, c.getAprobadoPor());
        assertNotNull(c.getFechaAprobacion());
    }

    @Test
    void aprobar_comisionLiquidada_lanzaIllegalState() {
        when(comisionRepository.findByIdParaActualizar(comisionId)).thenReturn(Optional.of(comision(EstadoComision.LIQUIDADA, "300.00")));
        assertThrows(IllegalStateException.class, () -> service.aprobar(comisionId));
        verify(comisionRepository, never()).save(any());
    }

    @Test
    void porEmpleado_multifuncionalVeLasSuyas() {
        actual = vendedor;
        when(comisionRepository.search(null, null, null, vendedor.getId(), Pageable.unpaged())).thenReturn(Page.empty());

        service.porEmpleado(vendedor.getId(), Pageable.unpaged());

        verify(comisionRepository).search(null, null, null, vendedor.getId(), Pageable.unpaged());
    }

    @Test
    void porEmpleado_multifuncionalAjeno_lanzaAccessDenied() {
        actual = vendedor;
        assertThrows(AccessDeniedException.class, () -> service.porEmpleado(UUID.randomUUID(), Pageable.unpaged()));
        verifyNoInteractions(comisionRepository);
    }

    // ===================== HELPERS =====================

    private User usuario(String nombre, RolNombre rol) {
        return User.builder().id(UUID.randomUUID()).nombre(nombre).apellido("Prueba").email(EMAIL)
                .activo(true).sede(sede).rol(Rol.builder().nombre(rol).build()).build();
    }

    private RrhhLegajoDigital legajo(String pct) {
        return RrhhLegajoDigital.builder().id(UUID.randomUUID()).user(vendedor).tipoContrato("PLANILLA")
                .sueldoBase(new BigDecimal("1500.00")).porcentajeComision(new BigDecimal(pct))
                .fechaIngreso(LocalDate.of(2024, 1, 1)).activo(true).build();
    }

    private RrhhComision comision(EstadoComision estado, String monto) {
        return RrhhComision.builder().id(comisionId).user(vendedor).sede(sede).periodo(PERIODO)
                .totalVentasNetas(new BigDecimal("3333.33")).cantidadVentas(5L)
                .porcentajeComision(new BigDecimal("3.00")).montoComision(new BigDecimal(monto)).estado(estado).build();
    }

    private VentasPorVendedorProjection ventas(UUID userId, String neto, long cantidad) {
        return new VentasPorVendedorProjection() {
            public UUID getUserId() { return userId; }
            public BigDecimal getNeto() { return new BigDecimal(neto); }
            public Long getCantidad() { return cantidad; }
        };
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void buscar_periodoValido_filtraYMapea() {
        RrhhComision c = comision(EstadoComision.CALCULADA, "100.00");
        Pageable p = PageRequest.of(0, 10);
        when(comisionRepository.search(PERIODO, sedeId, EstadoComision.CALCULADA, null, p)).thenReturn(new PageImpl<>(List.of(c)));

        assertEquals(1, service.buscar(PERIODO, sedeId, EstadoComision.CALCULADA, p).getTotalElements());
        verify(mapper).toResponseDto(c);
    }

    @Test
    void buscar_periodoEnBlanco_noFiltraPorPeriodo() {
        Pageable p = PageRequest.of(0, 10);
        when(comisionRepository.search(null, null, null, null, p)).thenReturn(Page.empty());

        assertTrue(service.buscar(" ", null, null, p).isEmpty());
    }

    @Test
    void buscar_periodoInvalido_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.buscar("septiembre", null, null, PageRequest.of(0, 10)));
        verifyNoInteractions(comisionRepository);
    }

    @Test
    void buscar_noAdmin_lanzaAccessDenied() {
        actual = vendedor;

        assertThrows(AccessDeniedException.class, () -> service.buscar(PERIODO, null, null, PageRequest.of(0, 10)));
    }

    @Test
    void porPeriodo_devuelveTodasLasDelPeriodoYSede() {
        RrhhComision a = comision(EstadoComision.CALCULADA, "100.00");
        RrhhComision b = comision(EstadoComision.APROBADA, "50.00");
        when(comisionRepository.findByPeriodo(PERIODO, sedeId)).thenReturn(List.of(a, b));

        assertEquals(2, service.porPeriodo(PERIODO, sedeId).size());
        verify(mapper).toResponseDto(a);
        verify(mapper).toResponseDto(b);
    }

    @Test
    void porPeriodo_periodoNulo_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.porPeriodo(null, sedeId));
        verifyNoInteractions(comisionRepository);
    }

    @Test
    void porPeriodo_noAdmin_lanzaAccessDenied() {
        actual = vendedor;

        assertThrows(AccessDeniedException.class, () -> service.porPeriodo(PERIODO, sedeId));
    }
}
