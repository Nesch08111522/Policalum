package com.policalum.erp.application.service.serviceImpl;

import org.springframework.data.domain.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.AsignarTurnoRequestDTO;
import com.policalum.erp.application.dto.request.TurnoTrabajoRequestDTO;
import com.policalum.erp.application.dto.response.AsignacionTurnoResponseDTO;
import com.policalum.erp.application.dto.response.TurnoDashboardDTO;
import com.policalum.erp.application.dto.response.TurnoDelDiaDTO;
import com.policalum.erp.application.mapper.TurnoTrabajoMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.util.HorarioUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
class TurnoTrabajoServiceImplTest {

    @Mock RrhhTurnoTrabajoRepository turnoRepository;
    @Mock RrhhLegajoDigitalRepository legajoRepository;
    @Mock UserRepository userRepository;
    @Mock SedeRepository sedeRepository;
    @Mock TurnoTrabajoMapper mapper;
    @Mock AuditService auditService;
    @InjectMocks TurnoTrabajoServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    private static final LocalTime H8 = LocalTime.of(8, 0), H14 = LocalTime.of(14, 0), H17 = LocalTime.of(17, 0);
    UUID turnoId = UUID.randomUUID();
    Sede sede;
    User actual;
    User empleado;

    @BeforeEach
    void setUp() {
        sede = Sede.builder().id(UUID.randomUUID()).nombre("Trujillo Centro").activa(true).build();
        actual = usuario("Miguel", RolNombre.ROLE_ADMIN, true);
        empleado = usuario("Ana", RolNombre.ROLE_MULTIFUNCIONAL, true);

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(actual));
        lenient().when(mapper.toEntity(any())).thenAnswer(i -> new RrhhTurnoTrabajo());
        lenient().when(turnoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== CREAR =====================

    @Test
    void crear_normalizaDiasYUsaSedeDelEmpleado() {
        // given
        when(userRepository.findById(empleado.getId())).thenReturn(Optional.of(empleado));
        when(turnoRepository.findActivosByUserId(empleado.getId())).thenReturn(List.of());
        ArgumentCaptor<RrhhTurnoTrabajo> captor = ArgumentCaptor.forClass(RrhhTurnoTrabajo.class);

        // when
        service.crear(request("mañana", H8, H14, List.of("viernes", "Miércoles", "LUNES", "lunes"), null));

        // then
        verify(turnoRepository).save(captor.capture());
        RrhhTurnoTrabajo t = captor.getValue();
        assertEquals(List.of("LUNES", "MIERCOLES", "VIERNES"), t.getDiasSemana());
        assertEquals("MAÑANA", t.getNombreTurno());
        assertEquals(10, t.getToleranciaMinutos());
        assertSame(sede, t.getSede());
        assertSame(empleado, t.getUser());
        assertTrue(t.getActivo());
        verify(auditService).registrar(eq("rrhh_turnos_trabajo"), any(), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void crear_turnoRotativoQueNoSeCruza_seAcepta() {
        // given: ya tiene L-M-V 08-14; el nuevo es SABADO 14-17
        when(userRepository.findById(empleado.getId())).thenReturn(Optional.of(empleado));
        when(turnoRepository.findActivosByUserId(empleado.getId()))
                .thenReturn(List.of(turno("MAÑANA", H8, H14, List.of("LUNES", "MIERCOLES", "VIERNES"))));

        // when
        service.crear(request("TARDE", H14, H17, List.of("SABADO"), 5));

        // then
        verify(turnoRepository).save(any());
    }

    @Test
    void crear_seCruzaConOtroTurno_lanzaIllegalState() {
        // given: ya tiene LUNES 08-14; el nuevo es LUNES 12-17
        when(userRepository.findById(empleado.getId())).thenReturn(Optional.of(empleado));
        when(turnoRepository.findActivosByUserId(empleado.getId()))
                .thenReturn(List.of(turno("MAÑANA", H8, H14, List.of("LUNES"))));

        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.crear(request("TARDE", LocalTime.of(12, 0), H17, List.of("LUNES"), null)));
        assertTrue(ex.getMessage().contains("se cruza"));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void crear_turnoQueEmpiezaJustoAlTerminarOtro_noEsCruce() {
        when(userRepository.findById(empleado.getId())).thenReturn(Optional.of(empleado));
        when(turnoRepository.findActivosByUserId(empleado.getId()))
                .thenReturn(List.of(turno("MAÑANA", H8, H14, List.of("LUNES"))));

        service.crear(request("TARDE", H14, H17, List.of("LUNES"), null));

        verify(turnoRepository).save(any());
    }

    @Test
    void crear_horaInicioIgualAFin_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(request("X", H8, H8, List.of("LUNES"), null)));
        verifyNoInteractions(turnoRepository);
    }

    @Test
    void crear_diaInvalido_lanzaIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.crear(request("X", H8, H14, List.of("FERIADO"), null)));
        assertTrue(ex.getMessage().contains("FERIADO"));
    }

    @Test
    void crear_toleranciaMayorQueLaDuracion_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(request("CORTO", H8, LocalTime.of(8, 30), List.of("LUNES"), 30)));
    }

    @Test
    void crear_empleadoInactivo_lanzaIllegalState() {
        empleado.setActivo(false);
        when(userRepository.findById(empleado.getId())).thenReturn(Optional.of(empleado));
        assertThrows(IllegalStateException.class, () -> service.crear(request("X", H8, H14, List.of("LUNES"), null)));
    }

    @Test
    void crear_usuarioNoAdmin_lanzaAccessDenied() {
        actual = usuario("Luis", RolNombre.ROLE_MULTIFUNCIONAL, true);
        assertThrows(AccessDeniedException.class, () -> service.crear(request("X", H8, H14, List.of("LUNES"), null)));
        verifyNoInteractions(turnoRepository);
    }

    // ===================== ACTUALIZAR / ELIMINAR =====================

    @Test
    void actualizar_ignoraElPropioTurnoAlValidarCruces() {
        // given: el único turno existente es el mismo que se edita
        RrhhTurnoTrabajo existente = turno("MAÑANA", H8, H14, List.of("LUNES"));
        when(turnoRepository.findDetalleById(turnoId)).thenReturn(Optional.of(existente));
        when(turnoRepository.findActivosByUserId(empleado.getId())).thenReturn(List.of(existente));

        // when: se amplía a 08-15
        service.actualizar(turnoId, request("MAÑANA", H8, LocalTime.of(15, 0), List.of("LUNES"), null));

        // then
        assertEquals(LocalTime.of(15, 0), existente.getHoraFin());
        verify(auditService).registrar(eq("rrhh_turnos_trabajo"), eq(turnoId), eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void actualizar_cambiarDeEmpleado_lanzaIllegalArgument() {
        when(turnoRepository.findDetalleById(turnoId)).thenReturn(Optional.of(turno("MAÑANA", H8, H14, List.of("LUNES"))));
        TurnoTrabajoRequestDTO otro = new TurnoTrabajoRequestDTO(UUID.randomUUID(), null, "MAÑANA", H8, H14, List.of("LUNES"), null, null);
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(turnoId, otro));
    }

    @Test
    void eliminar_haceBajaLogica() {
        RrhhTurnoTrabajo t = turno("MAÑANA", H8, H14, List.of("LUNES"));
        when(turnoRepository.findDetalleById(turnoId)).thenReturn(Optional.of(t));

        service.eliminar(turnoId);

        assertFalse(t.getActivo());
        verify(turnoRepository, never()).delete(any());
        verify(auditService).registrar(eq("rrhh_turnos_trabajo"), eq(turnoId), eq(AccionAudit.DELETE), anyMap());
    }

    @Test
    void eliminar_turnoYaInactivo_lanzaIllegalState() {
        RrhhTurnoTrabajo t = turno("MAÑANA", H8, H14, List.of("LUNES"));
        t.setActivo(false);
        when(turnoRepository.findDetalleById(turnoId)).thenReturn(Optional.of(t));
        assertThrows(IllegalStateException.class, () -> service.eliminar(turnoId));
    }

    // ===================== ASIGNAR =====================

    @Test
    void asignar_variosEmpleados_creaUnTurnoPorCada() {
        User otro = usuario("Beto", RolNombre.ROLE_MULTIFUNCIONAL, true);
        when(userRepository.findById(empleado.getId())).thenReturn(Optional.of(empleado));
        when(userRepository.findById(otro.getId())).thenReturn(Optional.of(otro));
        when(turnoRepository.findActivosByUserId(any())).thenReturn(List.of());

        AsignacionTurnoResponseDTO r = service.asignar(asignacion(List.of(empleado.getId(), otro.getId(), empleado.getId())));

        assertEquals(2, r.creados()); // el id repetido se ignora
        verify(turnoRepository, times(2)).save(any());
    }

    @Test
    void asignar_unoConCruce_noCreaNinguno() {
        User otro = usuario("Beto", RolNombre.ROLE_MULTIFUNCIONAL, true);
        when(userRepository.findById(empleado.getId())).thenReturn(Optional.of(empleado));
        when(userRepository.findById(otro.getId())).thenReturn(Optional.of(otro));
        when(turnoRepository.findActivosByUserId(empleado.getId())).thenReturn(List.of());
        when(turnoRepository.findActivosByUserId(otro.getId()))
                .thenReturn(List.of(turno("MAÑANA", H8, H14, List.of("LUNES"))));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.asignar(asignacion(List.of(empleado.getId(), otro.getId()))));
        assertTrue(ex.getMessage().contains("Beto"));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void asignar_empleadoInexistente_noCreaNinguno() {
        UUID fantasma = UUID.randomUUID();
        when(userRepository.findById(fantasma)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.asignar(asignacion(List.of(fantasma))));
        verify(turnoRepository, never()).save(any());
    }

    // ===================== CONSULTAS =====================

    @Test
    void porEmpleado_multifuncionalVeLosSuyos() {
        actual = empleado;
        when(turnoRepository.findActivosByUserId(empleado.getId())).thenReturn(List.of(turno("MAÑANA", H8, H14, List.of("LUNES"))));

        assertEquals(1, service.porEmpleado(empleado.getId()).size());
    }

    @Test
    void porEmpleado_multifuncionalAjeno_lanzaAccessDenied() {
        actual = usuario("Luis", RolNombre.ROLE_MULTIFUNCIONAL, true);
        assertThrows(AccessDeniedException.class, () -> service.porEmpleado(empleado.getId()));
        verifyNoInteractions(turnoRepository);
    }

    @Test
    void obtener_noExiste_lanzaNotFound() {
        when(turnoRepository.findDetalleById(turnoId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obtener(turnoId));
    }

    @Test
    void dashboard_coberturaSinTurnoYExcesoDe48h() {
        // given: Ana 08-17 de LUNES a SABADO = 54 h (excede 48); Beto sin turno pero con legajo
        RrhhTurnoTrabajo largo = turno("COMPLETO", H8, H17,
                List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO"));
        User beto = usuario("Beto", RolNombre.ROLE_MULTIFUNCIONAL, true);
        when(turnoRepository.findActivosPorSede(null)).thenReturn(List.of(largo));
        when(legajoRepository.findActivos(null)).thenReturn(List.of(
                RrhhLegajoDigital.builder().user(empleado).build(), RrhhLegajoDigital.builder().user(beto).build()));

        // when
        TurnoDashboardDTO d = service.dashboard(null);

        // then
        assertEquals(1, d.turnosActivos());
        assertEquals(1, d.empleadosConTurno());
        assertEquals(1, d.empleadosSinTurno());
        assertEquals(new BigDecimal("54.00"), d.horasSemanalesProgramadas());
        assertEquals(1L, d.coberturaPorDia().get("LUNES"));
        assertEquals(0L, d.coberturaPorDia().get("DOMINGO"));
        assertEquals(1, d.empleadosSobre48Horas().size());
        assertTrue(d.empleadosSobre48Horas().get(0).contains("54.00 h"));
    }

    // ===================== TURNO DEL DÍA (usado por asistencia) =====================

    @Test
    void turnoDelDia_eligeElTurnoMasCercanoALaMarcacion() {
        // given: lunes con dos turnos (08-12 y 14-18); marca a las 14:20 → turno de la tarde
        RrhhTurnoTrabajo manana = turno("MAÑANA", H8, LocalTime.of(12, 0), List.of("LUNES"));
        RrhhTurnoTrabajo tarde = turno("TARDE", H14, LocalTime.of(18, 0), List.of("LUNES"));
        tarde.setId(UUID.randomUUID());
        when(turnoRepository.findActivosByUserId(empleado.getId())).thenReturn(List.of(manana, tarde));
        LocalDateTime lunes1420 = LocalDate.of(2026, 10, 5).atTime(14, 20); // 5/10/2026 es lunes

        // when
        TurnoDelDiaDTO t = service.turnoDelDia(empleado.getId(), lunes1420).orElseThrow();

        // then
        assertEquals(tarde.getId(), t.turnoId());
        assertTrue(t.esTardanza(lunes1420.toLocalTime())); // 20 min > 10 de tolerancia
    }

    @Test
    void turnoDelDia_sinTurnoEseDia_devuelveVacio() {
        when(turnoRepository.findActivosByUserId(empleado.getId()))
                .thenReturn(List.of(turno("MAÑANA", H8, H14, List.of("Martes", "dato-invalido"))));
        assertTrue(service.turnoDelDia(empleado.getId(), LocalDate.of(2026, 10, 5).atTime(8, 0)).isEmpty());
    }

    // ===================== REGLAS DE HORARIO (unitarias) =====================

    @Test
    void tardanza_respetaToleranciaYCruceDeMedianoche() {
        TurnoDelDiaDTO dia = new TurnoDelDiaDTO(turnoId, "MAÑANA", H8, 10);
        assertFalse(dia.esTardanza(LocalTime.of(8, 10)));   // justo en la tolerancia
        assertTrue(dia.esTardanza(LocalTime.of(8, 11)));
        assertFalse(dia.esTardanza(LocalTime.of(7, 40)));   // llegó antes

        TurnoDelDiaDTO noche = new TurnoDelDiaDTO(turnoId, "NOCHE", LocalTime.of(23, 55), 10);
        assertFalse(noche.esTardanza(LocalTime.of(0, 3)));  // 8 min después, ya es otro día
        assertTrue(noche.esTardanza(LocalTime.of(0, 10)));  // 15 min
    }

    @Test
    void seCruzan_turnoNocturnoDomingoContraLunesTemprano() {
        // DOMINGO 22:00-06:00 termina el lunes a las 06:00; LUNES 05:00-09:00 se cruza
        assertTrue(HorarioUtil.seCruzan(List.of("DOMINGO"), LocalTime.of(22, 0), LocalTime.of(6, 0),
                List.of("LUNES"), LocalTime.of(5, 0), LocalTime.of(9, 0)));
        assertFalse(HorarioUtil.seCruzan(List.of("DOMINGO"), LocalTime.of(22, 0), LocalTime.of(6, 0),
                List.of("LUNES"), LocalTime.of(6, 0), LocalTime.of(9, 0)));
        assertEquals(480, HorarioUtil.duracionMinutos(LocalTime.of(22, 0), LocalTime.of(6, 0)));
    }

    // ===================== HELPERS =====================

    private User usuario(String nombre, RolNombre rol, boolean activo) {
        return User.builder().id(UUID.randomUUID()).nombre(nombre).apellido("Prueba").email(EMAIL)
                .activo(activo).sede(sede).rol(Rol.builder().nombre(rol).build()).build();
    }

    private TurnoTrabajoRequestDTO request(String nombre, LocalTime ini, LocalTime fin, List<String> dias, Integer tol) {
        return new TurnoTrabajoRequestDTO(empleado.getId(), null, nombre, ini, fin, dias, tol, null);
    }

    private AsignarTurnoRequestDTO asignacion(List<UUID> userIds) {
        return new AsignarTurnoRequestDTO(userIds, null, "MAÑANA", LocalTime.of(9, 0), LocalTime.of(13, 0),
                List.of("LUNES"), null);
    }

    private RrhhTurnoTrabajo turno(String nombre, LocalTime ini, LocalTime fin, List<String> dias) {
        return RrhhTurnoTrabajo.builder().id(turnoId).user(empleado).sede(sede).nombreTurno(nombre)
                .horaInicio(ini).horaFin(fin).diasSemana(new ArrayList<>(dias)).toleranciaMinutos(10).activo(true).build();
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void buscar_nombreNormalizadoComoPatronLike() {
        Pageable p = PageRequest.of(0, 10);
        when(turnoRepository.search(sede.getId(), null, true, "%mañana%", p)).thenReturn(Page.empty());

        service.buscar(sede.getId(), null, true, "  MAÑANA ", p);

        verify(turnoRepository).search(sede.getId(), null, true, "%mañana%", p);
    }

    @Test
    void buscar_sinNombre_usaComodinYMapea() {
        Pageable p = PageRequest.of(0, 10);
        RrhhTurnoTrabajo t = RrhhTurnoTrabajo.builder().id(turnoId).build();
        when(turnoRepository.search(null, null, null, "%", p)).thenReturn(new PageImpl<>(List.of(t)));

        assertEquals(1, service.buscar(null, null, null, " ", p).getTotalElements());
        verify(mapper).toResponseDto(t);
    }

    @Test
    void buscar_noAdmin_lanzaAccessDenied() {
        actual = empleado;

        assertThrows(AccessDeniedException.class, () -> service.buscar(null, null, null, null, PageRequest.of(0, 10)));
        verifyNoInteractions(turnoRepository);
    }
}
