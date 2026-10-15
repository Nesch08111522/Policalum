package com.policalum.erp.application.service.serviceImpl;

import org.springframework.data.domain.*;
import java.time.LocalDateTime;
import com.policalum.erp.application.dto.response.ReembolsoResponseDTO;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.ProcesarReembolsoRequestDTO;
import com.policalum.erp.application.dto.request.ReembolsoRequestDTO;
import com.policalum.erp.application.mapper.ReembolsoMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import java.math.BigDecimal;
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
class ReembolsoServiceImplTest {

    @Mock ReembolsoRepository reembolsoRepository;
    @Mock DevolucionRepository devolucionRepository;
    @Mock TurnoCajaRepository turnoRepository;
    @Mock PagoVentaRepository pagoVentaRepository;
    @Mock UserRepository userRepository;
    @Mock ReembolsoMapper mapper;
    @Mock AuditService auditService;
    @InjectMocks ReembolsoServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    UUID devId = UUID.randomUUID();
    UUID reembolsoId = UUID.randomUUID();
    UUID turnoId = UUID.randomUUID();
    Sede sede;
    User actual;
    Devolucion dev;

    @BeforeEach
    void setUp() {
        sede = Sede.builder().id(UUID.randomUUID()).nombre("Trujillo Centro").build();
        actual = usuario(RolNombre.ROLE_ADMIN);
        dev = Devolucion.builder().id(devId).codigo("DEV-2026-000001").sede(sede)
                .venta(Venta.builder().id(UUID.randomUUID()).build())
                .estado(EstadoDevolucion.APROBADA).montoTotal(new BigDecimal("238.00")).build();

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(actual));
        lenient().when(reembolsoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== CREAR =====================

    @Test
    void crear_sinMonto_usaElTotalDeLaDevolucion() {
        prepararDevolucion();
        ArgumentCaptor<Reembolso> captor = ArgumentCaptor.forClass(Reembolso.class);

        service.crear(new ReembolsoRequestDTO(devId, MetodoReembolso.YAPE, null, null, " OP-77 ", null));

        verify(reembolsoRepository).save(captor.capture());
        Reembolso r = captor.getValue();
        assertEquals(new BigDecimal("238.00"), r.getMontoTotalReembolso());
        assertEquals(new BigDecimal("238.00"), r.getMontoMaximo());
        assertNull(r.getMotivoAjuste());
        assertEquals(EstadoReembolso.PENDIENTE, r.getEstado());
        assertEquals("OP-77", r.getReferencia());
        assertSame(actual, r.getCreadoPor());
        assertNull(r.getProcesadoPor());
    }

    @Test
    void crear_montoMenorConMotivo_seAcepta() {
        prepararDevolucion();
        ArgumentCaptor<Reembolso> captor = ArgumentCaptor.forClass(Reembolso.class);

        service.crear(new ReembolsoRequestDTO(devId, MetodoReembolso.EFECTIVO, new BigDecimal("200"), "Producto usado", null, null));

        verify(reembolsoRepository).save(captor.capture());
        assertEquals(new BigDecimal("200.00"), captor.getValue().getMontoTotalReembolso());
        assertEquals("Producto usado", captor.getValue().getMotivoAjuste());
    }

    @Test
    void crear_montoMenorSinMotivo_lanzaIllegalArgument() {
        prepararDevolucion();
        assertThrows(IllegalArgumentException.class, () -> service.crear(
                new ReembolsoRequestDTO(devId, MetodoReembolso.EFECTIVO, new BigDecimal("200"), " ", null, null)));
        verify(reembolsoRepository, never()).save(any());
    }

    @Test
    void crear_montoMayorAlTope_lanzaIllegalArgument() {
        prepararDevolucion();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.crear(
                new ReembolsoRequestDTO(devId, MetodoReembolso.EFECTIVO, new BigDecimal("238.01"), null, null, null)));
        assertTrue(ex.getMessage().contains("no puede superar"));
    }

    @Test
    void crear_devolucionPendiente_lanzaIllegalState() {
        dev.setEstado(EstadoDevolucion.PENDIENTE);
        when(devolucionRepository.findByIdParaActualizar(devId)).thenReturn(Optional.of(dev));
        assertThrows(IllegalStateException.class, () -> service.crear(
                new ReembolsoRequestDTO(devId, MetodoReembolso.YAPE, null, null, null, null)));
    }

    @Test
    void crear_devolucionYaReembolsada_lanzaIllegalState() {
        when(devolucionRepository.findByIdParaActualizar(devId)).thenReturn(Optional.of(dev));
        when(reembolsoRepository.existsByDevolucion_Id(devId)).thenReturn(true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.crear(
                new ReembolsoRequestDTO(devId, MetodoReembolso.YAPE, null, null, null, null)));
        assertTrue(ex.getMessage().contains("ya tiene un reembolso"));
    }

    @Test
    void crear_devolucionNoExiste_lanzaNotFound() {
        when(devolucionRepository.findByIdParaActualizar(devId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.crear(
                new ReembolsoRequestDTO(devId, MetodoReembolso.YAPE, null, null, null, null)));
    }

    // ===================== PROCESAR =====================

    @Test
    void procesar_efectivo_saleDelTurnoAbierto() {
        // given: inicial 100 + ventas efectivo 300 - retiro 50 = 350 disponibles; reembolso 238
        Reembolso r = reembolso(MetodoReembolso.EFECTIVO, "238.00");
        TurnoCaja turno = turno("100.00", "50.00", null);
        when(reembolsoRepository.findByIdParaActualizar(reembolsoId)).thenReturn(Optional.of(r));
        when(turnoRepository.findParaActualizar(actual.getId(), sede.getId(), EstadoTurno.ABIERTO)).thenReturn(Optional.of(turno));
        when(pagoVentaRepository.sumMontoByTurnoAndMetodo(turnoId, MetodoPago.EFECTIVO)).thenReturn(new BigDecimal("300.00"));

        // when
        service.procesar(reembolsoId, new ProcesarReembolsoRequestDTO(null, "Entregado en tienda"));

        // then
        assertEquals(new BigDecimal("238.00"), turno.getTotalReembolsos());
        assertTrue(turno.getObservaciones().contains("[REEMBOLSO] -S/ 238.00 | DEV-2026-000001"));
        assertSame(turno, r.getTurnoCaja());
        assertEquals(EstadoReembolso.PROCESADO, r.getEstado());
        assertSame(actual, r.getProcesadoPor());
        assertNotNull(r.getFechaProcesado());
        assertEquals("Entregado en tienda", r.getObservaciones());
        verify(turnoRepository).save(turno);
        verify(auditService).registrar(eq("reembolsos"), eq(reembolsoId), eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void procesar_efectivoInsuficiente_cuentaRetirosYReembolsosPrevios() {
        // given: 100 + 300 - 50 retiros - 100 reembolsos = 250; pide 251
        Reembolso r = reembolso(MetodoReembolso.EFECTIVO, "251.00");
        TurnoCaja turno = turno("100.00", "50.00", "100.00");
        when(reembolsoRepository.findByIdParaActualizar(reembolsoId)).thenReturn(Optional.of(r));
        when(turnoRepository.findParaActualizar(actual.getId(), sede.getId(), EstadoTurno.ABIERTO)).thenReturn(Optional.of(turno));
        when(pagoVentaRepository.sumMontoByTurnoAndMetodo(turnoId, MetodoPago.EFECTIVO)).thenReturn(new BigDecimal("300.00"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.procesar(reembolsoId, null));
        assertTrue(ex.getMessage().contains("Disponible: S/ 250.00"));
        assertEquals(EstadoReembolso.PENDIENTE, r.getEstado());
        assertEquals(new BigDecimal("100.00"), turno.getTotalReembolsos());
    }

    @Test
    void procesar_efectivoSinTurnoAbierto_lanzaIllegalState() {
        when(reembolsoRepository.findByIdParaActualizar(reembolsoId)).thenReturn(Optional.of(reembolso(MetodoReembolso.EFECTIVO, "10.00")));
        when(turnoRepository.findParaActualizar(actual.getId(), sede.getId(), EstadoTurno.ABIERTO)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.procesar(reembolsoId, null));
    }

    @Test
    void procesar_transferencia_noTocaCajaYGuardaReferencia() {
        Reembolso r = reembolso(MetodoReembolso.TRANSFERENCIA, "238.00");
        when(reembolsoRepository.findByIdParaActualizar(reembolsoId)).thenReturn(Optional.of(r));

        service.procesar(reembolsoId, new ProcesarReembolsoRequestDTO(" TRX-9 ", null));

        assertEquals(EstadoReembolso.PROCESADO, r.getEstado());
        assertEquals("TRX-9", r.getReferencia());
        assertNull(r.getTurnoCaja());
        verifyNoInteractions(turnoRepository, pagoVentaRepository);
    }

    @Test
    void procesar_yaProcesado_lanzaIllegalState() {
        Reembolso r = reembolso(MetodoReembolso.YAPE, "10.00");
        r.setEstado(EstadoReembolso.PROCESADO);
        when(reembolsoRepository.findByIdParaActualizar(reembolsoId)).thenReturn(Optional.of(r));
        assertThrows(IllegalStateException.class, () -> service.procesar(reembolsoId, null));
    }

    @Test
    void procesar_multifuncional_lanzaAccessDenied() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL);
        assertThrows(AccessDeniedException.class, () -> service.procesar(reembolsoId, null));
        verifyNoInteractions(reembolsoRepository, turnoRepository);
    }

    // ===================== HELPERS =====================

    private void prepararDevolucion() {
        when(devolucionRepository.findByIdParaActualizar(devId)).thenReturn(Optional.of(dev));
        when(reembolsoRepository.existsByDevolucion_Id(devId)).thenReturn(false);
    }

    private User usuario(RolNombre rol) {
        return User.builder().id(UUID.randomUUID()).nombre("Miguel").apellido("Prueba").email(EMAIL)
                .activo(true).sede(sede).rol(Rol.builder().nombre(rol).build()).build();
    }

    private Reembolso reembolso(MetodoReembolso metodo, String monto) {
        return Reembolso.builder().id(reembolsoId).devolucion(dev).venta(dev.getVenta())
                .montoTotalReembolso(new BigDecimal(monto)).montoMaximo(new BigDecimal("238.00"))
                .metodoReembolso(metodo).estado(EstadoReembolso.PENDIENTE).build();
    }

    private TurnoCaja turno(String inicial, String retiros, String reembolsos) {
        return TurnoCaja.builder().id(turnoId).sede(sede).user(actual).estado(EstadoTurno.ABIERTO)
                .montoInicial(new BigDecimal(inicial))
                .totalRetirosSocios(retiros == null ? null : new BigDecimal(retiros))
                .totalReembolsos(reembolsos == null ? null : new BigDecimal(reembolsos)).build();
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void obtener_existente_mapea() {
        Reembolso r = reembolso(MetodoReembolso.EFECTIVO, "100.00");
        ReembolsoResponseDTO dto = mock(ReembolsoResponseDTO.class);
        when(reembolsoRepository.findDetalleById(reembolsoId)).thenReturn(Optional.of(r));
        when(mapper.toResponseDto(r)).thenReturn(dto);

        assertSame(dto, service.obtener(reembolsoId));
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(reembolsoRepository.findDetalleById(reembolsoId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(reembolsoId));
    }

    @Test
    void buscar_sinFechas_usaRangoAbiertoYFiltros() {
        Pageable p = PageRequest.of(0, 10);
        when(reembolsoRepository.search(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        service.buscar(null, EstadoReembolso.PENDIENTE, MetodoReembolso.YAPE, null, null, p);

        verify(reembolsoRepository).search(null, EstadoReembolso.PENDIENTE, MetodoReembolso.YAPE,
                LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.of(2100, 1, 1, 0, 0), p);
    }

    @Test
    void buscar_conFechas_hastaInclusivoYMapea() {
        Pageable p = PageRequest.of(0, 10);
        Reembolso r = reembolso(MetodoReembolso.EFECTIVO, "50.00");
        LocalDate d = LocalDate.of(2026, 9, 10);
        when(reembolsoRepository.search(null, null, null, d.atStartOfDay(), d.plusDays(1).atStartOfDay(), p))
                .thenReturn(new PageImpl<>(List.of(r)));

        assertEquals(1, service.buscar(null, null, null, d, d, p).getTotalElements());
        verify(mapper).toResponseDto(r);
    }

    @Test
    void buscar_desdePosteriorAHasta_lanzaIllegalArgument() {
        LocalDate hoy = LocalDate.now();

        assertThrows(IllegalArgumentException.class, () -> service.buscar(null, null, null, hoy, hoy.minusDays(1), PageRequest.of(0, 10)));
        verifyNoInteractions(reembolsoRepository);
    }
}
