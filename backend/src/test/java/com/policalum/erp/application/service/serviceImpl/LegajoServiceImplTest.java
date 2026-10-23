package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.LegajoRequestDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.LegajoDashboardDTO;
import com.policalum.erp.application.mapper.LegajoMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class LegajoServiceImplTest {

    @Mock RrhhLegajoDigitalRepository legajoRepository;
    @Mock RrhhLegajoDocumentoRepository documentoRepository;
    @Mock UserRepository userRepository;
    @Mock FileStorageService fileStorageService;
    @Mock LegajoMapper mapper;
    @Mock AuditService auditService;
    @InjectMocks LegajoServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    UUID legajoId = UUID.randomUUID();
    UUID docId = UUID.randomUUID();
    Sede sede;
    User actual;
    User empleado;

    @BeforeEach
    void setUp() {
        sede = Sede.builder().id(UUID.randomUUID()).nombre("Trujillo Centro").build();
        actual = usuario("Miguel", RolNombre.ROLE_ADMIN, true);
        empleado = usuario("Ana", RolNombre.ROLE_MULTIFUNCIONAL, true);

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(actual));

        lenient().when(mapper.toEntity(any())).thenAnswer(i -> {
            LegajoRequestDTO r = i.getArgument(0);
            return RrhhLegajoDigital.builder().fechaIngreso(r.fechaIngreso()).sueldoBase(r.sueldoBase())
                    .porcentajeComision(r.porcentajeComision()).sistemaPension(r.sistemaPension()).build();
        });
        lenient().when(legajoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(documentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== CREAR =====================

    @Test
    void crear_onpSinTasa_usaTasaPorDefecto13() {
        // given
        prepararAlta();
        ArgumentCaptor<RrhhLegajoDigital> captor = ArgumentCaptor.forClass(RrhhLegajoDigital.class);

        // when
        service.crear(request("planilla", "1500.00", "3.00", SistemaPension.ONP, null, LocalDate.of(2024, 3, 1)));

        // then
        verify(legajoRepository).save(captor.capture());
        RrhhLegajoDigital l = captor.getValue();
        assertSame(empleado, l.getUser());
        assertEquals("PLANILLA", l.getTipoContrato());
        assertEquals(new BigDecimal("13.00"), l.getTasaPension());
        assertTrue(l.getActivo());
        verify(auditService).registrar(eq("rrhh_legajo_digital"), any(), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void crear_afpConTasa_guardaLaTasa() {
        prepararAlta();
        ArgumentCaptor<RrhhLegajoDigital> captor = ArgumentCaptor.forClass(RrhhLegajoDigital.class);

        service.crear(request("PLANILLA", "2000.00", null, SistemaPension.AFP, "11.37", LocalDate.of(2024, 3, 1)));

        verify(legajoRepository).save(captor.capture());
        assertEquals(new BigDecimal("11.37"), captor.getValue().getTasaPension());
        assertEquals(BigDecimal.ZERO, captor.getValue().getPorcentajeComision());
    }

    @Test
    void crear_ningunoConTasaCero_guardaTasaNula() {
        prepararAlta();
        ArgumentCaptor<RrhhLegajoDigital> captor = ArgumentCaptor.forClass(RrhhLegajoDigital.class);

        service.crear(request("RECIBO_HONORARIOS", "1200.00", null, SistemaPension.NINGUNO, "0", LocalDate.of(2024, 3, 1)));

        verify(legajoRepository).save(captor.capture());
        assertNull(captor.getValue().getTasaPension());
    }

    @Test
    void crear_afpSinTasa_lanzaIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.crear(
                request("PLANILLA", "2000.00", null, SistemaPension.AFP, null, LocalDate.of(2024, 3, 1))));
        assertTrue(ex.getMessage().contains("AFP"));
        verify(legajoRepository, never()).save(any());
    }

    @Test
    void crear_ningunoConTasaPositiva_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(
                request("PLANILLA", "2000.00", null, SistemaPension.NINGUNO, "10.00", LocalDate.of(2024, 3, 1))));
    }

    @Test
    void crear_sueldoCero_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(
                request("PLANILLA", "0", null, SistemaPension.ONP, null, LocalDate.of(2024, 3, 1))));
    }

    @Test
    void crear_comisionMayorA100_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(
                request("PLANILLA", "1500.00", "100.01", SistemaPension.ONP, null, LocalDate.of(2024, 3, 1))));
    }

    @Test
    void crear_fechaIngresoFutura_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(
                request("PLANILLA", "1500.00", null, SistemaPension.ONP, null, LocalDate.now().plusDays(1))));
    }

    @Test
    void crear_tipoContratoInvalido_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(
                request("TEMPORAL", "1500.00", null, SistemaPension.ONP, null, LocalDate.of(2024, 3, 1))));
    }

    @Test
    void crear_empleadoInactivo_lanzaIllegalState() {
        empleado.setActivo(false);
        when(userRepository.findById(empleado.getId())).thenReturn(Optional.of(empleado));

        assertThrows(IllegalStateException.class, () -> service.crear(
                request("PLANILLA", "1500.00", null, SistemaPension.ONP, null, LocalDate.of(2024, 3, 1))));
        verify(legajoRepository, never()).save(any());
    }

    @Test
    void crear_empleadoYaTieneLegajo_lanzaIllegalState() {
        when(userRepository.findById(empleado.getId())).thenReturn(Optional.of(empleado));
        when(legajoRepository.existsByUser_Id(empleado.getId())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.crear(
                request("PLANILLA", "1500.00", null, SistemaPension.ONP, null, LocalDate.of(2024, 3, 1))));
        assertTrue(ex.getMessage().contains("ya tiene legajo"));
    }

    @Test
    void crear_empleadoNoExiste_lanzaNotFound() {
        when(userRepository.findById(empleado.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.crear(
                request("PLANILLA", "1500.00", null, SistemaPension.ONP, null, LocalDate.of(2024, 3, 1))));
    }

    @Test
    void crear_usuarioNoAdmin_lanzaAccessDenied() {
        actual = usuario("Luis", RolNombre.ROLE_MULTIFUNCIONAL, true);
        assertThrows(AccessDeniedException.class, () -> service.crear(
                request("PLANILLA", "1500.00", null, SistemaPension.ONP, null, LocalDate.of(2024, 3, 1))));
        verifyNoInteractions(legajoRepository);
    }

    // ===================== ACTUALIZAR / CONSULTAR =====================

    @Test
    void actualizar_cambiaAAfpYAudita() {
        // given
        RrhhLegajoDigital legajo = legajo();
        when(legajoRepository.findDetalleByUserId(empleado.getId())).thenReturn(Optional.of(legajo));
        when(documentoRepository.findByLegajo_IdOrderByCreatedAtDesc(legajoId)).thenReturn(List.of());

        // when
        service.actualizar(empleado.getId(),
                request("PLANILLA", "1800.00", "2.00", SistemaPension.AFP, "12.50", LocalDate.of(2024, 3, 1)));

        // then
        verify(mapper).updateEntityFromDto(any(), eq(legajo));
        assertEquals(new BigDecimal("12.50"), legajo.getTasaPension());
        assertTrue(legajo.getActivo());
        verify(auditService).registrar(eq("rrhh_legajo_digital"), eq(legajoId), eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void actualizar_empleadoIdDistintoAlDeLaUrl_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(UUID.randomUUID(),
                request("PLANILLA", "1800.00", null, SistemaPension.ONP, null, LocalDate.of(2024, 3, 1))));
        verifyNoInteractions(legajoRepository);
    }

    @Test
    void obtenerMio_sinLegajo_lanzaNotFound() {
        actual = empleado;
        when(legajoRepository.findDetalleByUserId(empleado.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerMio());
    }

    @Test
    void obtenerMio_empleadoVeSuLegajoConDocumentos() {
        actual = empleado;
        RrhhLegajoDigital legajo = legajo();
        when(legajoRepository.findDetalleByUserId(empleado.getId())).thenReturn(Optional.of(legajo));
        when(documentoRepository.findByLegajo_IdOrderByCreatedAtDesc(legajoId)).thenReturn(List.of(documento(legajo)));

        service.obtenerMio();

        verify(mapper).toResponseDto(eq(legajo), argThat(l -> l.size() == 1));
    }

    @Test
    void obtenerPorEmpleado_multifuncional_lanzaAccessDenied() {
        actual = empleado;
        assertThrows(AccessDeniedException.class, () -> service.obtenerPorEmpleado(UUID.randomUUID()));
    }

    @Test
    void buscar_textoNulo_usaPatronComodin() {
        Pageable pageable = Pageable.unpaged();
        when(legajoRepository.search(null, "CAS", null, true, "%", pageable)).thenReturn(Page.empty());

        service.buscar(null, " cas ", null, true, null, pageable);

        verify(legajoRepository).search(null, "CAS", null, true, "%", pageable);
    }

    // ===================== DOCUMENTOS =====================

    @Test
    void subirDocumento_guardaArchivoYMetadata() {
        // given
        RrhhLegajoDigital legajo = legajo();
        when(legajoRepository.findById(legajoId)).thenReturn(Optional.of(legajo));
        MockMultipartFile archivo = new MockMultipartFile("archivo", "C:\\fakepath\\dni.pdf", "application/pdf", new byte[]{1, 2, 3});
        when(fileStorageService.guardar(archivo, "legajos")).thenReturn("legajos/2026/10/x.pdf");
        ArgumentCaptor<RrhhLegajoDocumento> captor = ArgumentCaptor.forClass(RrhhLegajoDocumento.class);

        // when
        service.subirDocumento(legajoId, archivo, "dni", "  DNI vigente ");

        // then
        verify(documentoRepository).save(captor.capture());
        RrhhLegajoDocumento d = captor.getValue();
        assertEquals(TipoDocumentoLegajo.DNI, d.getTipo());
        assertEquals("dni.pdf", d.getNombreOriginal());
        assertEquals("legajos/2026/10/x.pdf", d.getRutaArchivo());
        assertEquals(3L, d.getTamanoBytes());
        assertEquals("DNI vigente", d.getDescripcion());
        assertSame(actual, d.getSubidoPor());
        verify(auditService).registrar(eq("rrhh_legajo_documentos"), any(), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void subirDocumento_tipoInvalido_noGuardaArchivo() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "x.pdf", "application/pdf", new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> service.subirDocumento(legajoId, archivo, "PASAPORTE", null));
        verifyNoInteractions(fileStorageService, documentoRepository);
    }

    @Test
    void subirDocumento_legajoNoExiste_noGuardaArchivo() {
        when(legajoRepository.findById(legajoId)).thenReturn(Optional.empty());
        MockMultipartFile archivo = new MockMultipartFile("archivo", "x.pdf", "application/pdf", new byte[]{1});
        assertThrows(ResourceNotFoundException.class, () -> service.subirDocumento(legajoId, archivo, "CV", null));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void descargarDocumento_duenoPuedeYQuedaAuditado() {
        // given
        actual = empleado;
        RrhhLegajoDocumento doc = documento(legajo());
        when(documentoRepository.findByIdAndLegajo_Id(docId, legajoId)).thenReturn(Optional.of(doc));
        when(fileStorageService.cargar("legajos/2026/10/x.pdf")).thenReturn(new ByteArrayResource(new byte[]{1}));

        // when
        ArchivoDescargaDTO r = service.descargarDocumento(legajoId, docId);

        // then
        assertEquals("contrato.pdf", r.nombreOriginal());
        assertEquals("application/pdf", r.contentType());
        verify(auditService).registrar(eq("rrhh_legajo_documentos"), eq(docId), eq(AccionAudit.ACCESS), anyMap());
    }

    @Test
    void descargarDocumento_deOtroEmpleado_lanzaAccessDenied() {
        actual = usuario("Luis", RolNombre.ROLE_MULTIFUNCIONAL, true);
        when(documentoRepository.findByIdAndLegajo_Id(docId, legajoId)).thenReturn(Optional.of(documento(legajo())));

        assertThrows(AccessDeniedException.class, () -> service.descargarDocumento(legajoId, docId));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void descargarDocumento_deOtroLegajo_lanzaNotFound() {
        when(documentoRepository.findByIdAndLegajo_Id(docId, legajoId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.descargarDocumento(legajoId, docId));
    }

    @Test
    void eliminarDocumento_borraRegistroYArchivo() {
        RrhhLegajoDocumento doc = documento(legajo());
        when(documentoRepository.findByIdAndLegajo_Id(docId, legajoId)).thenReturn(Optional.of(doc));

        service.eliminarDocumento(legajoId, docId);

        verify(documentoRepository).delete(doc);
        verify(fileStorageService).eliminar("legajos/2026/10/x.pdf"); // sin transacción activa: inmediato
        verify(auditService).registrar(eq("rrhh_legajo_documentos"), eq(docId), eq(AccionAudit.DELETE), anyMap());
    }

    // ===================== DASHBOARD =====================

    @Test
    void dashboard_calculaKpisYAlertas() {
        // given: 2 activos (uno sin sistema de pensión) y 1 inactivo; solo el primero tiene CONTRATO
        RrhhLegajoDigital a = legajo();
        RrhhLegajoDigital b = RrhhLegajoDigital.builder().id(UUID.randomUUID()).user(usuario("Beto", RolNombre.ROLE_MULTIFUNCIONAL, true))
                .tipoContrato("RECIBO_HONORARIOS").sueldoBase(new BigDecimal("1000.00")).activo(true).build();
        RrhhLegajoDigital c = RrhhLegajoDigital.builder().id(UUID.randomUUID()).user(usuario("Caro", RolNombre.ROLE_MULTIFUNCIONAL, false))
                .tipoContrato("PLANILLA").sueldoBase(new BigDecimal("3000.00")).activo(false).build();
        when(legajoRepository.findTodos(null)).thenReturn(List.of(a, b, c));
        when(documentoRepository.findLegajoIdsConTipo(TipoDocumentoLegajo.CONTRATO)).thenReturn(List.of(legajoId));
        when(documentoRepository.findLegajoIdsConTipo(TipoDocumentoLegajo.DNI)).thenReturn(List.of());
        when(legajoRepository.contarUsuariosActivosSinLegajo(null)).thenReturn(4L);
        when(documentoRepository.contarPorSede(null)).thenReturn(7L);

        // when
        LegajoDashboardDTO d = service.dashboard(null);

        // then
        assertEquals(3, d.totalLegajos());
        assertEquals(2, d.activos());
        assertEquals(1, d.inactivos());
        assertEquals(new BigDecimal("2500.00"), d.planillaMensualBase());
        assertEquals(new BigDecimal("1250.00"), d.sueldoPromedio());
        assertEquals(1, d.activosSinContrato());
        assertEquals(2, d.activosSinDni());
        assertEquals(1, d.activosSinSistemaPension());
        assertEquals(1L, d.activosPorTipoContrato().get("PLANILLA"));
        assertEquals(1L, d.activosPorSistemaPension().get("SIN_REGISTRAR"));
        assertEquals(4, d.usuariosActivosSinLegajo());
        assertEquals(7, d.totalDocumentos());
    }

    // ===================== REGLA DE PENSIÓN (unitaria) =====================

    @Test
    void resolverTasaPension_onpConTasaExplicita_laRespeta() {
        assertEquals(new BigDecimal("13.50"), LegajoServiceImpl.resolverTasaPension(SistemaPension.ONP, new BigDecimal("13.50")));
        assertEquals(new BigDecimal("13.00"), LegajoServiceImpl.resolverTasaPension(SistemaPension.ONP, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> LegajoServiceImpl.resolverTasaPension(null, null));
        assertThrows(IllegalArgumentException.class, () -> LegajoServiceImpl.resolverTasaPension(SistemaPension.AFP, new BigDecimal("-1")));
    }

    // ===================== HELPERS =====================

    private void prepararAlta() {
        when(userRepository.findById(empleado.getId())).thenReturn(Optional.of(empleado));
        when(legajoRepository.existsByUser_Id(empleado.getId())).thenReturn(false);
    }

    private User usuario(String nombre, RolNombre rol, boolean activo) {
        return User.builder().id(UUID.randomUUID()).nombre(nombre).apellido("Prueba").email(EMAIL)
                .activo(activo).sede(sede).rol(Rol.builder().nombre(rol).build()).build();
    }

    private LegajoRequestDTO request(String contrato, String sueldo, String comision, SistemaPension sistema,
                                     String tasa, LocalDate ingreso) {
        return new LegajoRequestDTO(empleado.getId(), contrato, ingreso, new BigDecimal(sueldo), null,
                comision == null ? null : new BigDecimal(comision), sistema,
                tasa == null ? null : new BigDecimal(tasa), null);
    }

    private RrhhLegajoDigital legajo() {
        return RrhhLegajoDigital.builder().id(legajoId).user(empleado).tipoContrato("PLANILLA")
                .fechaIngreso(LocalDate.of(2024, 3, 1)).sueldoBase(new BigDecimal("1500.00"))
                .porcentajeComision(new BigDecimal("3.00")).sistemaPension(SistemaPension.ONP)
                .tasaPension(new BigDecimal("13.00")).activo(true).build();
    }

    private RrhhLegajoDocumento documento(RrhhLegajoDigital legajo) {
        return RrhhLegajoDocumento.builder().id(docId).legajo(legajo).tipo(TipoDocumentoLegajo.CONTRATO)
                .nombreOriginal("contrato.pdf").rutaArchivo("legajos/2026/10/x.pdf")
                .contentType("application/pdf").tamanoBytes(1024L).subidoPor(actual).build();
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void listarDocumentos_admin_listaYMapea() {
        RrhhLegajoDigital l = legajo();
        RrhhLegajoDocumento d = documento(l);
        when(legajoRepository.findById(legajoId)).thenReturn(Optional.of(l));
        when(documentoRepository.findByLegajo_IdOrderByCreatedAtDesc(legajoId)).thenReturn(List.of(d));

        assertEquals(1, service.listarDocumentos(legajoId).size());
        verify(mapper).toDocumentoDto(d);
    }

    @Test
    void listarDocumentos_dueno_puedeVerSusDocumentos() {
        actual = empleado;
        when(legajoRepository.findById(legajoId)).thenReturn(Optional.of(legajo()));
        when(documentoRepository.findByLegajo_IdOrderByCreatedAtDesc(legajoId)).thenReturn(List.of());

        assertTrue(service.listarDocumentos(legajoId).isEmpty());
    }

    @Test
    void listarDocumentos_otroEmpleado_lanzaAccessDeniedSinListar() {
        actual = usuario("Luis", RolNombre.ROLE_MULTIFUNCIONAL, true);
        when(legajoRepository.findById(legajoId)).thenReturn(Optional.of(legajo()));

        assertThrows(AccessDeniedException.class, () -> service.listarDocumentos(legajoId));
        verifyNoInteractions(documentoRepository);
    }

    @Test
    void listarDocumentos_legajoInexistente_lanzaNotFound() {
        when(legajoRepository.findById(legajoId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listarDocumentos(legajoId));
    }
}
