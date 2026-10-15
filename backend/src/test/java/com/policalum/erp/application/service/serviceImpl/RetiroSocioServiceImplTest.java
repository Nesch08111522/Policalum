package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.exception.ResourceNotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.AnularRetiroRequestDTO;
import com.policalum.erp.application.dto.request.RetiroSocioRequestDTO;
import com.policalum.erp.application.dto.response.RetiroSocioDashboardDTO;
import com.policalum.erp.application.mapper.RetiroSocioMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.infrastructure.repository.projection.RetiroPorOrigenProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RetiroSocioServiceImplTest {

    @Mock FinanzasRetiroSocioRepository retiroRepository;
    @Mock TurnoCajaRepository turnoRepository;
    @Mock PagoVentaRepository pagoVentaRepository;
    @Mock SedeRepository sedeRepository;
    @Mock UserRepository userRepository;
    @Mock RetiroSocioMapper mapper;
    @Mock AuditService auditService;
    @InjectMocks RetiroSocioServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    UUID sedeId = UUID.randomUUID();
    UUID turnoId = UUID.randomUUID();
    UUID retiroId = UUID.randomUUID();
    Sede sede;
    User admin;

    @BeforeEach
    void setUp() {
        sede = Sede.builder().id(sedeId).nombre("Trujillo Centro").activa(true).build();
        admin = usuario(RolNombre.ROLE_ADMIN);

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(admin));

        lenient().when(mapper.toEntity(any())).thenAnswer(i -> {
            RetiroSocioRequestDTO dto = i.getArgument(0);
            return FinanzasRetiroSocio.builder().montoRetirado(dto.montoRetirado())
                    .origenFondos(dto.origenFondos()).descripcion(dto.descripcion()).build();
        });
        lenient().when(retiroRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== REGISTRAR =====================

    @Test
    void registrar_cajaFisica_vinculaTurnoYDescuentaDelEfectivo() {
        // given: inicial 100 + ventas efectivo 50 = 150 disponibles
        TurnoCaja turno = turno(EstadoTurno.ABIERTO, "100.00", "0.00");
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        when(turnoRepository.findParaActualizar(admin.getId(), sedeId, EstadoTurno.ABIERTO)).thenReturn(Optional.of(turno));
        when(pagoVentaRepository.sumMontoByTurnoAndMetodo(turnoId, MetodoPago.EFECTIVO)).thenReturn(new BigDecimal("50.00"));
        ArgumentCaptor<FinanzasRetiroSocio> captor = ArgumentCaptor.forClass(FinanzasRetiroSocio.class);

        // when
        service.registrar(request("80", OrigenFondos.CAJA_FISICA, sedeId));

        // then
        verify(retiroRepository).save(captor.capture());
        FinanzasRetiroSocio retiro = captor.getValue();
        assertSame(turno, retiro.getTurnoCaja());
        assertSame(admin, retiro.getAdmin());
        assertEquals(EstadoRetiro.REGISTRADO, retiro.getEstado());
        assertEquals(new BigDecimal("80.00"), retiro.getMontoRetirado());
        assertNotNull(retiro.getFechaRetiro());
        assertEquals(new BigDecimal("80.00"), turno.getTotalRetirosSocios());
        assertTrue(turno.getObservaciones().contains("[RETIRO SOCIO] -S/ 80.00"));
        verify(turnoRepository).save(turno);
    }

    @Test
    void registrar_cajaFisica_sinTurnoAbierto_lanzaIllegalState() {
        // given
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        when(turnoRepository.findParaActualizar(admin.getId(), sedeId, EstadoTurno.ABIERTO)).thenReturn(Optional.empty());

        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.registrar(request("50", OrigenFondos.CAJA_FISICA, sedeId)));
        assertTrue(ex.getMessage().contains("turno de caja ABIERTO"));
        verify(retiroRepository, never()).save(any());
    }

    @Test
    void registrar_cajaFisica_efectivoInsuficiente_lanzaIllegalState() {
        // given: 100 + 50 - 20 ya retirado = 130 disponibles
        TurnoCaja turno = turno(EstadoTurno.ABIERTO, "100.00", "20.00");
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        when(turnoRepository.findParaActualizar(admin.getId(), sedeId, EstadoTurno.ABIERTO)).thenReturn(Optional.of(turno));
        when(pagoVentaRepository.sumMontoByTurnoAndMetodo(turnoId, MetodoPago.EFECTIVO)).thenReturn(new BigDecimal("50.00"));

        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.registrar(request("130.01", OrigenFondos.CAJA_FISICA, sedeId)));
        assertTrue(ex.getMessage().contains("Disponible: S/ 130.00"));
        assertEquals(new BigDecimal("20.00"), turno.getTotalRetirosSocios());
        verify(retiroRepository, never()).save(any());
    }

    @Test
    void registrar_cajaFisica_turnoAntiguoConRetirosNull_tomaCero() {
        // given: turno creado antes del Módulo 5 (columna en null)
        TurnoCaja turno = turno(EstadoTurno.ABIERTO, "100.00", null);
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        when(turnoRepository.findParaActualizar(admin.getId(), sedeId, EstadoTurno.ABIERTO)).thenReturn(Optional.of(turno));
        when(pagoVentaRepository.sumMontoByTurnoAndMetodo(turnoId, MetodoPago.EFECTIVO)).thenReturn(null);

        // when
        service.registrar(request("100", OrigenFondos.CAJA_FISICA, sedeId));

        // then
        assertEquals(new BigDecimal("100.00"), turno.getTotalRetirosSocios());
    }

    @Test
    void registrar_cuentaBancaria_noTocaCaja() {
        // given
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        ArgumentCaptor<FinanzasRetiroSocio> captor = ArgumentCaptor.forClass(FinanzasRetiroSocio.class);

        // when
        service.registrar(request("5000", OrigenFondos.CUENTA_BANCARIA, sedeId));

        // then
        verify(retiroRepository).save(captor.capture());
        assertNull(captor.getValue().getTurnoCaja());
        verifyNoInteractions(turnoRepository, pagoVentaRepository);
    }

    @Test
    void registrar_sinSede_usaLaSedeDelAdmin() {
        // given
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        ArgumentCaptor<FinanzasRetiroSocio> captor = ArgumentCaptor.forClass(FinanzasRetiroSocio.class);

        // when
        service.registrar(request("10", OrigenFondos.CUENTA_BANCARIA, null));

        // then
        verify(retiroRepository).save(captor.capture());
        assertSame(sede, captor.getValue().getSede());
    }

    @Test
    void registrar_sedeInactiva_lanzaIllegalState() {
        // given
        sede.setActiva(false);
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(sede));

        // when / then
        assertThrows(IllegalStateException.class,
                () -> service.registrar(request("10", OrigenFondos.CUENTA_BANCARIA, sedeId)));
    }

    @Test
    void registrar_usuarioMultifuncional_lanzaAccessDenied() {
        // given
        admin = usuario(RolNombre.ROLE_MULTIFUNCIONAL);

        // when / then
        assertThrows(AccessDeniedException.class,
                () -> service.registrar(request("10", OrigenFondos.CUENTA_BANCARIA, sedeId)));
        verifyNoInteractions(retiroRepository, turnoRepository);
    }

    @Test
    void registrar_registraAuditoriaInsert() {
        // given
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(sede));

        // when
        service.registrar(request("10", OrigenFondos.CUENTA_BANCARIA, sedeId));

        // then
        verify(auditService).registrar(eq("finanzas_retiros_socio"), any(), eq(AccionAudit.INSERT), anyMap());
    }

    // ===================== ANULAR =====================

    @Test
    void anular_turnoAbierto_revierteElImpactoEnCaja() {
        // given
        TurnoCaja turno = turno(EstadoTurno.ABIERTO, "100.00", "80.00");
        FinanzasRetiroSocio retiro = retiro(EstadoRetiro.REGISTRADO, "80.00", turno);
        when(retiroRepository.findDetalleById(retiroId)).thenReturn(Optional.of(retiro));
        when(turnoRepository.findByIdParaActualizar(turnoId)).thenReturn(Optional.of(turno));

        // when
        service.anular(retiroId, new AnularRetiroRequestDTO("  Error de digitación  "));

        // then
        assertEquals(0, turno.getTotalRetirosSocios().compareTo(BigDecimal.ZERO));
        assertTrue(turno.getObservaciones().contains("[ANULACIÓN RETIRO SOCIO] +S/ 80.00"));
        assertEquals(EstadoRetiro.ANULADO, retiro.getEstado());
        assertEquals("Error de digitación", retiro.getMotivoAnulacion());
        assertSame(admin, retiro.getAnuladoPor());
        assertNotNull(retiro.getFechaAnulacion());
        assertTrue(retiro.getImpactoCajaRevertido());
        verify(auditService).registrar(eq("finanzas_retiros_socio"), eq(retiroId), eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void anular_turnoCerrado_anulaPeroNoRevierteCaja() {
        // given
        TurnoCaja turno = turno(EstadoTurno.CERRADO, "100.00", "80.00");
        FinanzasRetiroSocio retiro = retiro(EstadoRetiro.REGISTRADO, "80.00", turno);
        when(retiroRepository.findDetalleById(retiroId)).thenReturn(Optional.of(retiro));
        when(turnoRepository.findByIdParaActualizar(turnoId)).thenReturn(Optional.of(turno));

        // when
        service.anular(retiroId, new AnularRetiroRequestDTO("Devolución posterior"));

        // then
        assertEquals(new BigDecimal("80.00"), turno.getTotalRetirosSocios());
        assertEquals(EstadoRetiro.ANULADO, retiro.getEstado());
        assertFalse(retiro.getImpactoCajaRevertido());
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void anular_retiroBancario_noConsultaTurno() {
        // given
        FinanzasRetiroSocio retiro = retiro(EstadoRetiro.REGISTRADO, "500.00", null);
        when(retiroRepository.findDetalleById(retiroId)).thenReturn(Optional.of(retiro));

        // when
        service.anular(retiroId, new AnularRetiroRequestDTO("Duplicado"));

        // then
        assertEquals(EstadoRetiro.ANULADO, retiro.getEstado());
        assertFalse(retiro.getImpactoCajaRevertido());
        verifyNoInteractions(turnoRepository);
    }

    @Test
    void anular_retiroYaAnulado_lanzaIllegalState() {
        // given
        when(retiroRepository.findDetalleById(retiroId))
                .thenReturn(Optional.of(retiro(EstadoRetiro.ANULADO, "80.00", null)));

        // when / then
        assertThrows(IllegalStateException.class,
                () -> service.anular(retiroId, new AnularRetiroRequestDTO("Otra vez")));
        verify(retiroRepository, never()).save(any());
    }

    // ===================== CONSULTAS =====================

    @Test
    void buscar_desdePosteriorAHasta_lanzaIllegalArgument() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> service.buscar(null, null, null, null,
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), Pageable.unpaged()));
    }

    @Test
    void dashboard_sumaTotalesPorOrigen() {
        // given
        when(retiroRepository.totalesPorOrigen(eq(EstadoRetiro.REGISTRADO), eq(sedeId), any(), any()))
                .thenReturn(List.of(origen(OrigenFondos.CAJA_FISICA, 3, "300.00"),
                        origen(OrigenFondos.CUENTA_BANCARIA, 1, "1000.00")));
        when(retiroRepository.totalesPorAdmin(eq(EstadoRetiro.REGISTRADO), eq(sedeId), any(), any())).thenReturn(List.of());
        when(retiroRepository.contarPorEstado(eq(EstadoRetiro.ANULADO), eq(sedeId), any(), any())).thenReturn(2L);

        // when
        RetiroSocioDashboardDTO dto = service.dashboard(sedeId, null, null);

        // then
        assertEquals(new BigDecimal("1300.00"), dto.totalRetirado());
        assertEquals(new BigDecimal("300.00"), dto.totalCajaFisica());
        assertEquals(new BigDecimal("1000.00"), dto.totalCuentaBancaria());
        assertEquals(4, dto.cantidadRetiros());
        assertEquals(2, dto.cantidadAnulados());
        assertEquals(LocalDate.now().withDayOfMonth(1), dto.desde());
    }

    // ===================== HELPERS =====================

    private User usuario(RolNombre rol) {
        return User.builder().id(UUID.randomUUID()).nombre("Miguel").apellido("Dueño").email(EMAIL)
                .activo(true).sede(sede).rol(Rol.builder().nombre(rol).build()).build();
    }

    private TurnoCaja turno(EstadoTurno estado, String inicial, String retiros) {
        return TurnoCaja.builder().id(turnoId).sede(sede).user(admin).estado(estado)
                .montoInicial(new BigDecimal(inicial))
                .totalRetirosSocios(retiros == null ? null : new BigDecimal(retiros)).build();
    }

    private FinanzasRetiroSocio retiro(EstadoRetiro estado, String monto, TurnoCaja turno) {
        return FinanzasRetiroSocio.builder().id(retiroId).admin(admin).sede(sede).turnoCaja(turno)
                .montoRetirado(new BigDecimal(monto))
                .origenFondos(turno != null ? OrigenFondos.CAJA_FISICA : OrigenFondos.CUENTA_BANCARIA)
                .estado(estado).impactoCajaRevertido(false).build();
    }

    private RetiroSocioRequestDTO request(String monto, OrigenFondos origen, UUID sede) {
        return new RetiroSocioRequestDTO(new BigDecimal(monto), origen, sede, "Retiro de prueba");
    }

    private RetiroPorOrigenProjection origen(OrigenFondos origen, long cantidad, String total) {
        return new RetiroPorOrigenProjection() {
            public OrigenFondos getOrigen() { return origen; }
            public Long getCantidad() { return cantidad; }
            public BigDecimal getTotal() { return new BigDecimal(total); }
        };
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void obtener_admin_buscaConDetalleYMapea() {
        FinanzasRetiroSocio r = retiro(EstadoRetiro.REGISTRADO, "120.00", null);
        when(retiroRepository.findDetalleById(retiroId)).thenReturn(Optional.of(r));

        service.obtener(retiroId);

        verify(mapper).toResponseDto(r);
    }

    @Test
    void obtener_noAdmin_lanzaAccessDeniedSinConsultar() {
        admin = usuario(RolNombre.ROLE_MULTIFUNCIONAL);

        assertThrows(AccessDeniedException.class, () -> service.obtener(retiroId));
        verifyNoInteractions(retiroRepository);
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(retiroRepository.findDetalleById(retiroId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(retiroId));
    }
}
