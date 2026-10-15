package com.policalum.erp.application.service.serviceImpl;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.AsignarRepartidorRequestDTO;
import com.policalum.erp.application.dto.request.CambiarEstadoDespachoRequestDTO;
import com.policalum.erp.application.dto.request.CrearDespachoRequestDTO;
import com.policalum.erp.application.dto.request.EvidenciaEntregaRequestDTO;
import com.policalum.erp.application.dto.response.DespachoDashboardDTO;
import com.policalum.erp.application.mapper.OrdenDespachoMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.CorrelativoService;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.infrastructure.repository.projection.DespachosPorEstadoProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class OrdenDespachoServiceImplTest {

    @Mock OrdenDespachoRepository despachoRepository;
    @Mock VentaRepository ventaRepository;
    @Mock SedeRepository sedeRepository;
    @Mock UserRepository userRepository;
    @Mock MovilidadRepository movilidadRepository;
    @Mock CorrelativoService correlativoService;
    @Mock FileStorageService fileStorageService;
    @Mock OrdenDespachoMapper mapper;
    @Mock AuditService auditService;
    @InjectMocks OrdenDespachoServiceImpl service;

    private static final String EMAIL = "user@policalum.com";
    UUID ordenId = UUID.randomUUID();
    UUID ventaId = UUID.randomUUID();
    Sede sede;
    User actual;
    User repartidor;
    Venta venta;

    @BeforeEach
    void setUp() {
        sede = Sede.builder().id(UUID.randomUUID()).nombre("Trujillo Centro").activa(true).build();
        actual = usuario("Miguel", RolNombre.ROLE_ADMIN);
        repartidor = usuario("Pedro", RolNombre.ROLE_MULTIFUNCIONAL);
        venta = Venta.builder().id(ventaId).sede(sede).estado(EstadoVenta.COMPLETADA).build();

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(actual));
        lenient().when(mapper.toEntity(any())).thenAnswer(i -> new OrdenDespacho());
        lenient().when(despachoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== CREAR =====================

    @Test
    void crear_generaCodigoCortoYQuedaPendienteParaHoy() {
        // given
        when(ventaRepository.findById(ventaId)).thenReturn(Optional.of(venta));
        when(correlativoService.siguiente("DESP-" + Year.now().getValue())).thenReturn(123L);
        ArgumentCaptor<OrdenDespacho> captor = ArgumentCaptor.forClass(OrdenDespacho.class);

        // when
        service.crear(crear(null));

        // then
        verify(despachoRepository).save(captor.capture());
        OrdenDespacho o = captor.getValue();
        assertEquals("DESP-" + Year.now().getValue() + "-000123", o.getCodigoSeguimiento());
        assertEquals(EstadoDespacho.PENDIENTE, o.getEstado());
        assertEquals(LocalDate.now(), o.getFechaProgramada());
        assertSame(sede, o.getSede());
        assertSame(venta, o.getVenta());
        assertSame(actual, o.getCreadoPor());
        assertEquals("Av. España 123", o.getDireccionEntrega());
        verify(auditService).registrar(eq("ordenes_despacho"), any(), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void crear_ventaAnulada_lanzaIllegalState() {
        venta.setEstado(EstadoVenta.ANULADA);
        when(ventaRepository.findById(ventaId)).thenReturn(Optional.of(venta));

        assertThrows(IllegalStateException.class, () -> service.crear(crear(null)));
        verifyNoInteractions(correlativoService);
        verify(despachoRepository, never()).save(any());
    }

    @Test
    void crear_ventaNoExiste_lanzaNotFound() {
        when(ventaRepository.findById(ventaId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.crear(crear(null)));
    }

    @Test
    void crear_fechaPasada_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(crear(LocalDate.now().minusDays(1))));
        verifyNoInteractions(ventaRepository);
    }

    // ===================== ASIGNAR =====================

    @Test
    void asignar_sinMovilidad_tomaLaUnicaDelConductor() {
        // given
        OrdenDespacho o = orden(EstadoDespacho.PENDIENTE, null);
        Movilidad moto = Movilidad.builder().id(UUID.randomUUID()).placa("M1-234").sede(sede).activo(true).conductor(repartidor).build();
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(o));
        when(userRepository.findById(repartidor.getId())).thenReturn(Optional.of(repartidor));
        when(movilidadRepository.findByConductor_IdAndActivoTrue(repartidor.getId())).thenReturn(List.of(moto));

        // when
        service.asignarRepartidor(ordenId, new AsignarRepartidorRequestDTO(repartidor.getId(), null));

        // then
        assertSame(repartidor, o.getResponsable());
        assertSame(moto, o.getMovilidad());
        assertNotNull(o.getFechaAsignacion());
    }

    @Test
    void asignar_movilidadDeOtraSede_lanzaIllegalArgument() {
        OrdenDespacho o = orden(EstadoDespacho.PENDIENTE, null);
        Sede otra = Sede.builder().id(UUID.randomUUID()).nombre("Otra").build();
        Movilidad camion = Movilidad.builder().id(UUID.randomUUID()).placa("C1-111").sede(otra).activo(true).build();
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(o));
        when(userRepository.findById(repartidor.getId())).thenReturn(Optional.of(repartidor));
        when(movilidadRepository.findById(camion.getId())).thenReturn(Optional.of(camion));

        assertThrows(IllegalArgumentException.class,
                () -> service.asignarRepartidor(ordenId, new AsignarRepartidorRequestDTO(repartidor.getId(), camion.getId())));
        assertNull(o.getResponsable());
    }

    @Test
    void asignar_ordenEnCamino_lanzaIllegalState() {
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(orden(EstadoDespacho.EN_CAMINO, repartidor)));
        assertThrows(IllegalStateException.class,
                () -> service.asignarRepartidor(ordenId, new AsignarRepartidorRequestDTO(repartidor.getId(), null)));
    }

    @Test
    void asignar_repartidorInactivo_lanzaIllegalState() {
        repartidor.setActivo(false);
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(orden(EstadoDespacho.PENDIENTE, null)));
        when(userRepository.findById(repartidor.getId())).thenReturn(Optional.of(repartidor));
        assertThrows(IllegalStateException.class,
                () -> service.asignarRepartidor(ordenId, new AsignarRepartidorRequestDTO(repartidor.getId(), null)));
    }

    // ===================== ESTADOS =====================

    @Test
    void cambiarEstado_repartidorSaleARuta() {
        // given: el repartidor asignado marca EN_CAMINO
        actual = repartidor;
        OrdenDespacho o = orden(EstadoDespacho.PENDIENTE, repartidor);
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(o));

        // when
        service.cambiarEstado(ordenId, new CambiarEstadoDespachoRequestDTO(EstadoDespacho.EN_CAMINO, null, null));

        // then
        assertEquals(EstadoDespacho.EN_CAMINO, o.getEstado());
        assertNotNull(o.getFechaSalida());
        verify(auditService).registrar(eq("ordenes_despacho"), eq(ordenId), eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void cambiarEstado_enCaminoSinRepartidor_lanzaIllegalState() {
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(orden(EstadoDespacho.PENDIENTE, null)));
        assertThrows(IllegalStateException.class,
                () -> service.cambiarEstado(ordenId, new CambiarEstadoDespachoRequestDTO(EstadoDespacho.EN_CAMINO, null, null)));
    }

    @Test
    void cambiarEstado_fallidoSinMotivo_lanzaIllegalArgument() {
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(orden(EstadoDespacho.EN_CAMINO, repartidor)));
        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(ordenId, new CambiarEstadoDespachoRequestDTO(EstadoDespacho.FALLIDO, " ", null)));
    }

    @Test
    void cambiarEstado_fallido_sumaIntentoYGuardaMotivo() {
        OrdenDespacho o = orden(EstadoDespacho.EN_CAMINO, repartidor);
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(o));

        service.cambiarEstado(ordenId, new CambiarEstadoDespachoRequestDTO(EstadoDespacho.FALLIDO, "Cliente ausente", null));

        assertEquals(EstadoDespacho.FALLIDO, o.getEstado());
        assertEquals(1, o.getIntentosFallidos());
        assertEquals("Cliente ausente", o.getMotivoFallo());
    }

    @Test
    void cambiarEstado_reprogramarFallido_vuelveAPendienteConNuevaFecha() {
        OrdenDespacho o = orden(EstadoDespacho.FALLIDO, repartidor);
        o.setFechaSalida(java.time.LocalDateTime.now().minusHours(3));
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(o));
        LocalDate manana = LocalDate.now().plusDays(1);

        service.cambiarEstado(ordenId, new CambiarEstadoDespachoRequestDTO(EstadoDespacho.PENDIENTE, null, manana));

        assertEquals(EstadoDespacho.PENDIENTE, o.getEstado());
        assertEquals(manana, o.getFechaProgramada());
        assertNull(o.getFechaSalida());
    }

    @Test
    void cambiarEstado_reprogramarConFechaPasada_lanzaIllegalArgument() {
        OrdenDespacho o = orden(EstadoDespacho.FALLIDO, repartidor);
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(o));

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(ordenId,
                        new CambiarEstadoDespachoRequestDTO(EstadoDespacho.PENDIENTE, null,
                                LocalDate.now().minusDays(1))));
        assertEquals(EstadoDespacho.FALLIDO, o.getEstado()); // no cambió
        assertEquals(LocalDate.now(), o.getFechaProgramada()); // tampoco la fecha
        verify(despachoRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_aEntregado_debeUsarEvidencia() {
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(orden(EstadoDespacho.EN_CAMINO, repartidor)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(ordenId, new CambiarEstadoDespachoRequestDTO(EstadoDespacho.ENTREGADO, null, null)));
        assertTrue(ex.getMessage().contains("evidencia"));
    }

    @Test
    void cambiarEstado_desdeEntregado_lanzaIllegalState() {
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(orden(EstadoDespacho.ENTREGADO, repartidor)));
        assertThrows(IllegalStateException.class,
                () -> service.cambiarEstado(ordenId, new CambiarEstadoDespachoRequestDTO(EstadoDespacho.PENDIENTE, null, null)));
    }

    @Test
    void cambiarEstado_multifuncionalNoAsignado_lanzaAccessDenied() {
        actual = usuario("Otro", RolNombre.ROLE_MULTIFUNCIONAL);
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(orden(EstadoDespacho.PENDIENTE, repartidor)));
        assertThrows(AccessDeniedException.class,
                () -> service.cambiarEstado(ordenId, new CambiarEstadoDespachoRequestDTO(EstadoDespacho.EN_CAMINO, null, null)));
    }

    // ===================== ENTREGA =====================

    @Test
    void registrarEntrega_guardaFotoQuienRecibeYGeo() {
        // given
        actual = repartidor;
        OrdenDespacho o = orden(EstadoDespacho.EN_CAMINO, repartidor);
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(o));
        MockMultipartFile foto = foto();
        when(fileStorageService.guardar(foto, "despachos")).thenReturn("despachos/2026/10/x.jpg");

        // when
        service.registrarEntrega(ordenId, foto, new EvidenciaEntregaRequestDTO(" Rosa Díaz ", "12345678",
                new BigDecimal("-8.111900"), new BigDecimal("-79.028700"), "Dejado en portería"));

        // then
        assertEquals(EstadoDespacho.ENTREGADO, o.getEstado());
        assertNotNull(o.getFechaEntrega());
        assertEquals("despachos/2026/10/x.jpg", o.getUrlEvidenciaEntrega());
        assertEquals("Rosa Díaz", o.getRecibidoPorNombre());
        assertEquals("12345678", o.getRecibidoPorDocumento());
        assertEquals(new BigDecimal("-8.111900"), o.getLatitudEntrega());
        assertTrue(o.getObservaciones().contains("[ENTREGA] Dejado en portería"));
        assertEquals("image/jpeg", o.getEvidenciaContentType());
    }

    @Test
    void registrarEntrega_sinFoto_noTocaNada() {
        MockMultipartFile vacia = new MockMultipartFile("archivo", "x.jpg", "image/jpeg", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> service.registrarEntrega(ordenId, vacia,
                new EvidenciaEntregaRequestDTO("Rosa", null, null, null, null)));
        verifyNoInteractions(despachoRepository, fileStorageService);
    }

    @Test
    void registrarEntrega_sinNombreDeQuienRecibe_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.registrarEntrega(ordenId, foto(),
                new EvidenciaEntregaRequestDTO("  ", null, null, null, null)));
    }

    @Test
    void registrarEntrega_soloLatitud_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.registrarEntrega(ordenId, foto(),
                new EvidenciaEntregaRequestDTO("Rosa", null, new BigDecimal("-8.1"), null, null)));
    }

    @Test
    void registrarEntrega_dniInvalido_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.registrarEntrega(ordenId, foto(),
                new EvidenciaEntregaRequestDTO("Rosa", "123", null, null, null)));
    }

    @Test
    void registrarEntrega_ordenPendiente_noGuardaArchivo() {
        when(despachoRepository.findByIdParaActualizar(ordenId)).thenReturn(Optional.of(orden(EstadoDespacho.PENDIENTE, repartidor)));
        assertThrows(IllegalStateException.class, () -> service.registrarEntrega(ordenId, foto(),
                new EvidenciaEntregaRequestDTO("Rosa", null, null, null, null)));
        verifyNoInteractions(fileStorageService);
    }

    // ===================== CONSULTAS =====================

    @Test
    void seguimiento_codigoInexistente_lanzaNotFound() {
        when(despachoRepository.findByCodigo("DESP-2026-999999")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.seguimiento(" DESP-2026-999999 "));
    }

    @Test
    void buscar_sinFechas_incluyeOrdenesSinFechaProgramada() {
        org.springframework.data.domain.Pageable p = org.springframework.data.domain.Pageable.unpaged();
        when(despachoRepository.search(null, null, null, null, OrdenDespachoServiceImpl.FECHA_MIN,
                OrdenDespachoServiceImpl.FECHA_MAX, true, p)).thenReturn(org.springframework.data.domain.Page.empty());

        service.buscar(null, null, null, null, null, null, p);

        verify(despachoRepository).search(null, null, null, null, OrdenDespachoServiceImpl.FECHA_MIN,
                OrdenDespachoServiceImpl.FECHA_MAX, true, p);
    }

    @Test
    void dashboard_tasaPrimerIntentoYEstadosEnCero() {
        // given: 4 entregas del día, 3 sin fallos previos → 75.00 %
        LocalDate hoy = LocalDate.now();
        when(despachoRepository.contarPorEstadoEnFecha(null, hoy)).thenReturn(List.of(porEstado(EstadoDespacho.PENDIENTE, 5)));
        when(despachoRepository.contarEntregados(eq(null), any(), any())).thenReturn(4L);
        when(despachoRepository.contarEntregadosPrimerIntento(eq(null), any(), any())).thenReturn(3L);
        when(despachoRepository.cargaPorRepartidor(null)).thenReturn(List.of());
        when(despachoRepository.contarAtrasados(null, hoy)).thenReturn(2L);

        // when
        DespachoDashboardDTO d = service.dashboard(null, null);

        // then
        assertEquals(new BigDecimal("75.00"), d.tasaEntregaPrimerIntento());
        assertEquals(5L, d.programadosPorEstado().get("PENDIENTE"));
        assertEquals(0L, d.programadosPorEstado().get("FALLIDO"));
        assertEquals(2, d.atrasados());
    }

    // ===================== HELPERS =====================

    private User usuario(String nombre, RolNombre rol) {
        return User.builder().id(UUID.randomUUID()).nombre(nombre).apellido("Prueba").email(EMAIL)
                .activo(true).sede(sede).rol(Rol.builder().nombre(rol).build()).build();
    }

    private CrearDespachoRequestDTO crear(LocalDate fecha) {
        return new CrearDespachoRequestDTO(ventaId, null, "  Av. España 123 ", "Frente al parque",
                "Rosa", "949123456", fecha, null);
    }

    private OrdenDespacho orden(EstadoDespacho estado, User responsable) {
        return OrdenDespacho.builder().id(ordenId).venta(venta).sede(sede).responsable(responsable)
                .direccionEntrega("Av. España 123").estado(estado).fechaProgramada(LocalDate.now())
                .codigoSeguimiento("DESP-2026-000001").intentosFallidos(0).build();
    }

    private MockMultipartFile foto() {
        return new MockMultipartFile("archivo", "entrega.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private DespachosPorEstadoProjection porEstado(EstadoDespacho e, long n) {
        return new DespachosPorEstadoProjection() {
            public EstadoDespacho getEstado() { return e; }
            public Long getCantidad() { return n; }
        };
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void obtener_existente_mapea() {
        OrdenDespacho o = orden(EstadoDespacho.PENDIENTE, repartidor);
        when(despachoRepository.findDetalleById(ordenId)).thenReturn(Optional.of(o));

        service.obtener(ordenId);

        verify(mapper).toResponseDto(o);
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(despachoRepository.findDetalleById(ordenId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(ordenId));
    }

    @Test
    void descargarEvidencia_conEvidencia_devuelveArchivoConNombreYTipoOriginal() {
        OrdenDespacho o = orden(EstadoDespacho.ENTREGADO, repartidor);
        o.setUrlEvidenciaEntrega("despachos/abc.jpg");
        o.setEvidenciaNombreOriginal("firma.jpg");
        o.setEvidenciaContentType("image/jpeg");
        Resource res = new ByteArrayResource(new byte[] {1, 2, 3});
        when(despachoRepository.findDetalleById(ordenId)).thenReturn(Optional.of(o));
        when(fileStorageService.cargar("despachos/abc.jpg")).thenReturn(res);

        ArchivoDescargaDTO a = service.descargarEvidencia(ordenId);

        assertSame(res, a.resource());
        assertEquals("firma.jpg", a.nombreOriginal());
        assertEquals("image/jpeg", a.contentType());
    }

    @Test
    void descargarEvidencia_sinNombreOriginal_usaCodigoDeSeguimiento() {
        OrdenDespacho o = orden(EstadoDespacho.ENTREGADO, repartidor);
        o.setUrlEvidenciaEntrega("despachos/abc.jpg");
        when(despachoRepository.findDetalleById(ordenId)).thenReturn(Optional.of(o));
        when(fileStorageService.cargar(any())).thenReturn(new ByteArrayResource(new byte[0]));

        assertEquals("evidencia-DESP-2026-000001", service.descargarEvidencia(ordenId).nombreOriginal());
    }

    @Test
    void descargarEvidencia_sinEvidencia_lanzaNotFoundSinTocarDisco() {
        when(despachoRepository.findDetalleById(ordenId)).thenReturn(Optional.of(orden(EstadoDespacho.EN_CAMINO, repartidor)));

        assertThrows(ResourceNotFoundException.class, () -> service.descargarEvidencia(ordenId));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void porRepartidor_soloActivas_filtraPendientesYEnCamino() {
        OrdenDespacho o = orden(EstadoDespacho.EN_CAMINO, repartidor);
        when(despachoRepository.findPorResponsable(repartidor.getId(), EnumSet.of(EstadoDespacho.PENDIENTE, EstadoDespacho.EN_CAMINO)))
                .thenReturn(List.of(o));

        assertEquals(1, service.porRepartidor(repartidor.getId(), true).size());
        verify(mapper).toResponseDto(o);
    }

    @Test
    void porRepartidor_todas_pideTodosLosEstados() {
        when(despachoRepository.findPorResponsable(repartidor.getId(), EnumSet.allOf(EstadoDespacho.class))).thenReturn(List.of());

        assertTrue(service.porRepartidor(repartidor.getId(), false).isEmpty());
    }
}
