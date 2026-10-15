package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.AsignarConductorRequestDTO;
import com.policalum.erp.application.dto.request.MovilidadRequestDTO;
import com.policalum.erp.application.dto.response.MovilidadDashboardDTO;
import com.policalum.erp.application.dto.response.MovilidadResponseDTO;
import com.policalum.erp.application.dto.response.ReporteUsoMovilidadDTO;
import com.policalum.erp.application.mapper.MovilidadMapper;
import com.policalum.erp.application.mapper.OrdenDespachoMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.infrastructure.repository.projection.ConteoPorTextoProjection;
import com.policalum.erp.infrastructure.repository.projection.UsoMovilidadProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MovilidadServiceImplTest {

    @Mock MovilidadRepository movilidadRepository;
    @Mock OrdenDespachoRepository despachoRepository;
    @Mock SedeRepository sedeRepository;
    @Mock UserRepository userRepository;
    @Spy MovilidadMapper mapper = Mappers.getMapper(MovilidadMapper.class);
    @Mock OrdenDespachoMapper despachoMapper;
    @Mock AuditService auditService;
    @InjectMocks MovilidadServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    private static final List<EstadoDespacho> ACTIVOS = List.of(EstadoDespacho.PENDIENTE, EstadoDespacho.EN_CAMINO);
    User admin;
    Sede centro, norte;
    Movilidad moto;

    @BeforeEach
    void setUp() {
        centro = Sede.builder().id(UUID.randomUUID()).nombre("Centro").build();
        norte = Sede.builder().id(UUID.randomUUID()).nombre("Norte").build();
        admin = usuario(RolNombre.ROLE_ADMIN, true);
        admin.setEmail(EMAIL);
        moto = Movilidad.builder().id(UUID.randomUUID()).placa("1234-AB").tipo("MOTO").sede(centro).activo(true).build();

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(admin));
        lenient().when(sedeRepository.findById(centro.getId())).thenReturn(Optional.of(centro));
        lenient().when(sedeRepository.findById(norte.getId())).thenReturn(Optional.of(norte));
        lenient().when(movilidadRepository.findByIdParaActualizar(moto.getId())).thenReturn(Optional.of(moto));
        lenient().when(movilidadRepository.save(any())).thenAnswer(i -> {
            Movilidad m = i.getArgument(0);
            if (m.getId() == null) m.setId(UUID.randomUUID());
            return m;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== CREAR / EDITAR =====================

    @Test
    void crear_normalizaPlacaQuedaActivoYAudita() {
        ArgumentCaptor<Movilidad> captor = ArgumentCaptor.forClass(Movilidad.class);
        when(movilidadRepository.existePlaca(eq("ABC123"), any())).thenReturn(false);

        MovilidadResponseDTO r = service.crear(new MovilidadRequestDTO(centro.getId(), " abc123 ", TipoMovilidad.CAMIONETA, "Toyota", "Hilux", null));

        verify(movilidadRepository).save(captor.capture());
        Movilidad m = captor.getValue();
        assertEquals("ABC-123", m.getPlaca());
        assertEquals("CAMIONETA", m.getTipo());
        assertTrue(m.getActivo());
        assertSame(centro, m.getSede());
        assertNull(m.getConductor());
        assertEquals("Centro", r.sedeNombre());
        verify(auditService).registrar(eq("movilidades"), eq(m.getId()), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void crear_placaRepetidaEnOtraSede_lanzaIllegalState() {
        // la placa es única en toda la empresa: "abc-123" ya existe como "ABC 123" en otra sede
        when(movilidadRepository.existePlaca(eq("ABC123"), any())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.crear(new MovilidadRequestDTO(norte.getId(), "abc-123", TipoMovilidad.AUTO, null, null, null)));
        assertTrue(ex.getMessage().contains("ABC-123"));
        verify(movilidadRepository, never()).save(any());
    }

    @Test
    void crear_noAdmin_lanzaAccessDenied() {
        admin.setRol(Rol.builder().nombre(RolNombre.ROLE_MULTIFUNCIONAL).build());
        assertThrows(AccessDeniedException.class,
                () -> service.crear(new MovilidadRequestDTO(centro.getId(), "ABC123", TipoMovilidad.AUTO, null, null, null)));
        verifyNoInteractions(movilidadRepository);
    }

    @Test
    void actualizar_excluyeSuPropiaPlacaYNoPermiteDesactivarConRutas() {
        when(movilidadRepository.existePlaca("1234AB", moto.getId())).thenReturn(false);
        when(despachoRepository.countByMovilidad_IdAndEstadoIn(moto.getId(), ACTIVOS)).thenReturn(0L);

        MovilidadResponseDTO r = service.actualizar(moto.getId(), new MovilidadRequestDTO(centro.getId(), "1234-ab", TipoMovilidad.MOTO, "Honda", "XR150", null));
        assertEquals("Honda", r.marca());
        assertTrue(r.activo());

        when(despachoRepository.countByMovilidad_IdAndEstadoIn(moto.getId(), ACTIVOS)).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> service.actualizar(moto.getId(),
                new MovilidadRequestDTO(centro.getId(), "1234-AB", TipoMovilidad.MOTO, null, null, false)));
        assertTrue(moto.getActivo());
    }

    // ===================== DESACTIVAR =====================

    @Test
    void desactivar_conDespachosActivos_lanzaIllegalState() {
        when(despachoRepository.countByMovilidad_IdAndEstadoIn(moto.getId(), ACTIVOS)).thenReturn(2L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.desactivar(moto.getId()));
        assertTrue(ex.getMessage().contains("2 despacho(s)"));
        assertTrue(moto.getActivo());
        verify(movilidadRepository, never()).save(any());
    }

    @Test
    void desactivar_sinDespachosActivos_bajaLogicaConBloqueo() {
        when(despachoRepository.countByMovilidad_IdAndEstadoIn(moto.getId(), ACTIVOS)).thenReturn(0L);

        service.desactivar(moto.getId());

        assertFalse(moto.getActivo());
        verify(movilidadRepository).findByIdParaActualizar(moto.getId());
        verify(movilidadRepository, never()).delete(any());
        verify(auditService).registrar(eq("movilidades"), eq(moto.getId()), eq(AccionAudit.DELETE), anyMap());
    }

    @Test
    void desactivar_yaInactiva_lanzaIllegalState() {
        moto.setActivo(false);
        assertThrows(IllegalStateException.class, () -> service.desactivar(moto.getId()));
        verifyNoInteractions(despachoRepository);
    }

    // ===================== CONDUCTOR =====================

    @Test
    void asignarConductor_multifuncionalActivo_seAsignaYQuitarConNull() {
        User repartidor = usuario(RolNombre.ROLE_MULTIFUNCIONAL, true);
        when(userRepository.findById(repartidor.getId())).thenReturn(Optional.of(repartidor));

        MovilidadResponseDTO r = service.asignarConductor(moto.getId(), new AsignarConductorRequestDTO(repartidor.getId()));
        assertEquals(repartidor.getId(), r.conductorId());
        assertEquals("Luis Pérez", r.conductorNombre());

        service.asignarConductor(moto.getId(), new AsignarConductorRequestDTO(null));
        assertNull(moto.getConductor());
        verify(auditService, times(2)).registrar(eq("movilidades"), eq(moto.getId()), eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void asignarConductor_inactivoOAdmin_lanzaIllegalArgument() {
        User inactivo = usuario(RolNombre.ROLE_MULTIFUNCIONAL, false);
        User otroAdmin = usuario(RolNombre.ROLE_ADMIN, true);
        when(userRepository.findById(inactivo.getId())).thenReturn(Optional.of(inactivo));
        when(userRepository.findById(otroAdmin.getId())).thenReturn(Optional.of(otroAdmin));

        assertThrows(IllegalArgumentException.class, () -> service.asignarConductor(moto.getId(), new AsignarConductorRequestDTO(inactivo.getId())));
        assertThrows(IllegalArgumentException.class, () -> service.asignarConductor(moto.getId(), new AsignarConductorRequestDTO(otroAdmin.getId())));
        assertNull(moto.getConductor());
    }

    @Test
    void asignarConductor_vehiculoInactivo_lanzaIllegalState() {
        moto.setActivo(false);
        assertThrows(IllegalStateException.class, () -> service.asignarConductor(moto.getId(), new AsignarConductorRequestDTO(UUID.randomUUID())));
        verify(userRepository, never()).findById(any());
    }

    // ===================== CONSULTAS =====================

    @Test
    void historial_vehiculoInexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(movilidadRepository.existsById(id)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.historial(id, null, PageRequest.of(0, 20)));
    }

    @Test
    void listar_normalizaTextoYTipo() {
        when(movilidadRepository.buscar(any(), any(), any(), any(), any())).thenReturn(Page.empty());

        service.listar(centro.getId(), true, TipoMovilidad.MOTO, "  Honda ", PageRequest.of(0, 20, Sort.by("marca")));

        verify(movilidadRepository).buscar(centro.getId(), true, "MOTO", "%honda%", PageRequest.of(0, 20));
    }

    @Test
    void reporteUso_tasaDeExitoOrdenadoPorEntregas() {
        LocalDate d = LocalDate.of(2026, 10, 1), h = LocalDate.of(2026, 10, 31);
        when(movilidadRepository.uso(null, d, h)).thenReturn(List.of(
                uso("AAA-111", 3, 1, 0), uso("BBB-222", 8, 2, 1), uso("CCC-333", 0, 0, 0)));
        when(despachoRepository.contarSinMovilidad(null, d, h)).thenReturn(4L);

        ReporteUsoMovilidadDTO r = service.reporteUso(null, d, h);

        assertEquals("BBB-222", r.vehiculos().get(0).placa());
        assertEquals(new BigDecimal("80.00"), r.vehiculos().get(0).tasaExitoPorcentaje());
        assertNull(r.vehiculos().get(2).tasaExitoPorcentaje());
        assertEquals(11, r.totalEntregados());
        assertEquals(3, r.totalFallidos());
        assertEquals(4, r.despachosSinMovilidad());
        assertThrows(IllegalArgumentException.class, () -> service.reporteUso(null, h, d));
    }

    @Test
    void dashboard_cuentaFlotaYCompletaTipos() {
        when(movilidadRepository.contarPorEstado(null, true)).thenReturn(5L);
        when(movilidadRepository.contarPorEstado(null, false)).thenReturn(2L);
        when(movilidadRepository.contarActivosSinConductor(null)).thenReturn(1L);
        when(movilidadRepository.activosPorTipo(null)).thenReturn(List.of(tipo("MOTO", 3), tipo("CAMIONETA", 2)));
        when(despachoRepository.contarVehiculosEnRuta(null)).thenReturn(2L);
        when(despachoRepository.contarEntregasConMovilidad(any(), any(), any())).thenReturn(9L);

        MovilidadDashboardDTO r = service.dashboard(null);

        assertEquals(7, r.total());
        assertEquals(0L, r.activosPorTipo().get("CAMION"));
        assertEquals(3L, r.activosPorTipo().get("MOTO"));
        assertEquals(2, r.enRuta());
        assertEquals(9, r.entregasHoy());
    }

    @Test
    void formatearPlaca_casos() {
        assertEquals("ABC-123", MovilidadServiceImpl.formatearPlaca("abc 123"));
        assertEquals("A1B-234", MovilidadServiceImpl.formatearPlaca("a1b234"));
        assertEquals("1234-AB", MovilidadServiceImpl.formatearPlaca("1234-ab"));
        assertEquals("ABC123", MovilidadServiceImpl.normalizarPlaca("abc-123"));
    }

    // ===================== HELPERS =====================

    private User usuario(RolNombre rol, boolean activo) {
        return User.builder().id(UUID.randomUUID()).nombre("Luis").apellido("Pérez").email(UUID.randomUUID() + "@policalum.com")
                .activo(activo).rol(Rol.builder().nombre(rol).build()).build();
    }

    private UsoMovilidadProjection uso(String placa, long entregados, long fallidos, long pendientes) {
        return new UsoMovilidadProjection() {
            public UUID getMovilidadId() { return UUID.randomUUID(); }
            public String getPlaca() { return placa; }
            public String getTipo() { return "MOTO"; }
            public String getConductorNombre() { return null; }
            public String getConductorApellido() { return null; }
            public Boolean getActivo() { return true; }
            public Long getDespachos() { return entregados + fallidos + pendientes; }
            public Long getEntregados() { return entregados; }
            public Long getFallidos() { return fallidos; }
            public Long getPendientes() { return pendientes; }
            public Long getIntentosFallidos() { return 0L; }
        };
    }

    private ConteoPorTextoProjection tipo(String clave, long n) {
        return new ConteoPorTextoProjection() {
            public String getClave() { return clave; }
            public Long getCantidad() { return n; }
        };
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void obtener_incluyeConteoDeOrdenesActivas() {
        when(movilidadRepository.findDetalleById(moto.getId())).thenReturn(Optional.of(moto));
        when(despachoRepository.countByMovilidad_IdAndEstadoIn(moto.getId(), List.of(EstadoDespacho.PENDIENTE, EstadoDespacho.EN_CAMINO))).thenReturn(2L);

        MovilidadResponseDTO r = service.obtener(moto.getId());

        assertEquals("1234-AB", r.placa());
        assertEquals(2L, r.ordenesActivas());
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(movilidadRepository.findDetalleById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(id));
        verifyNoInteractions(despachoRepository);
    }
}
