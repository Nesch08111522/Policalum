package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.AuditoriaDashboardDTO;
import com.policalum.erp.application.dto.response.LogAuditoriaResponseDTO;
import com.policalum.erp.application.dto.response.ParametrosReportesDTO;
import com.policalum.erp.application.mapper.LogAuditoriaMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.AuditLog;
import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.AuditLogRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.projection.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceImplTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock UserRepository userRepository;
    @Mock LogAuditoriaMapper mapper;
    @Mock AuditService auditService;
    @Mock ConfiguracionService configuracionService;
    @InjectMocks AuditoriaServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    User admin;

    @BeforeEach
    void setUp() {
        lenient().when(configuracionService.reportes()).thenReturn(ParametrosReportesDTO.DEFAULTS);
        admin = User.builder().id(UUID.randomUUID()).nombre("Miguel").apellido("Dueño").email(EMAIL)
                .rol(Rol.builder().nombre(RolNombre.ROLE_ADMIN).build()).build();
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(admin));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== CONSULTAS =====================

    @Test
    void buscar_sinFechas_usaRangoAmplioIncluyeLogsSinFechaYNormalizaTabla() {
        Pageable pedido = PageRequest.of(2, 15, Sort.by("tabla"));
        when(auditLogRepository.buscar(any(), any(), any(), any(), any(), any(), anyBoolean(), any())).thenReturn(Page.empty());

        service.buscar("  VENTAS ", AccionAudit.UPDATE, null, null, null, pedido);

        verify(auditLogRepository).buscar("ventas", null, null, AccionAudit.UPDATE,
                LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.of(2100, 1, 2, 0, 0), true, PageRequest.of(2, 15));
    }

    @Test
    void buscar_conFechas_rangoSemiabiertoYExcluyeLogsSinFecha() {
        UUID userId = UUID.randomUUID();
        AuditLog log = AuditLog.builder().id(UUID.randomUUID()).tabla("ventas").build();
        LogAuditoriaResponseDTO dto = new LogAuditoriaResponseDTO(log.getId(), "ventas", null, null, null, null, null, null, null, null);
        when(auditLogRepository.buscar(isNull(), isNull(), eq(userId), isNull(), any(), any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(log)));
        when(mapper.toResponseDto(log)).thenReturn(dto);

        Page<LogAuditoriaResponseDTO> r = service.buscar(" ", null, userId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 8), PageRequest.of(0, 20));

        assertEquals(List.of(dto), r.getContent());
        verify(auditLogRepository).buscar(null, null, userId, null,
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 9, 0, 0), false, PageRequest.of(0, 20));
    }

    @Test
    void buscar_desdePosteriorAHasta_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.buscar(null, null, null,
                LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 1), PageRequest.of(0, 20)));
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(auditLogRepository.findDetalleById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obtener(id));
    }

    @Test
    void porUsuario_usuarioInexistente_lanzaNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.porUsuario(userId, null, null, null, PageRequest.of(0, 20)));
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void porEntidad_normalizaTablaYExigeQueVenga() {
        UUID registro = UUID.randomUUID();
        when(auditLogRepository.findByTablaAndRegistroIdOrderByCreatedAtDesc("preformas_venta", registro)).thenReturn(List.of());

        assertTrue(service.porEntidad("Preformas_Venta", registro).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.porEntidad("  ", registro));
    }

    @Test
    void consultas_usuarioNoAdmin_lanzaAccessDenied() {
        admin.setRol(Rol.builder().nombre(RolNombre.ROLE_MULTIFUNCIONAL).build());
        assertThrows(AccessDeniedException.class, () -> service.buscar(null, null, null, null, null, PageRequest.of(0, 20)));
        assertThrows(AccessDeniedException.class, () -> service.exportarCsv(null, null, null, null, null));
        assertThrows(AccessDeniedException.class, () -> service.dashboard(null, null));
        verifyNoInteractions(auditLogRepository, auditService);
    }

    // ===================== EXPORTAR =====================

    @Test
    void exportar_generaCsvConBomCabeceraEscapesYQuedaAuditado() throws Exception {
        // given: un log con coma y comillas en el user agent, otro del sistema sin fecha
        User cajera = User.builder().id(UUID.randomUUID()).nombre("Ana").apellido("Ruiz").email("ana@policalum.com").build();
        AuditLog conUsuario = AuditLog.builder().id(UUID.randomUUID()).tabla("ventas").registroId(UUID.randomUUID())
                .accion(AccionAudit.INSERT).user(cajera).userAgent("Mozilla \"5.0\", Win")
                .datosJson(new LinkedHashMap<>(Map.of("total", 118))).createdAt(LocalDateTime.of(2026, 10, 8, 9, 30, 5)).build();
        AuditLog sistema = AuditLog.builder().id(UUID.randomUUID()).tabla("preformas_venta").registroId(UUID.randomUUID())
                .accion(AccionAudit.UPDATE).build();
        when(auditLogRepository.buscarParaExportar(any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(conUsuario, sistema), PageRequest.of(0, 1000), 2));

        // when
        ArchivoDescargaDTO archivo = service.exportarCsv(null, null, null, null, null);

        // then
        String csv = new String(archivo.resource().getContentAsByteArray(), StandardCharsets.UTF_8);
        String[] lineas = csv.split("\r\n");
        assertTrue(csv.startsWith("\uFEFF"));
        assertEquals("\uFEFFfecha,accion,tabla,registro_id,usuario_id,usuario,email,user_agent,datos", lineas[0]);
        assertTrue(lineas[1].startsWith("2026-10-08 09:30:05,INSERT,ventas,"));
        assertTrue(lineas[1].contains(",Ana Ruiz,ana@policalum.com,\"Mozilla \"\"5.0\"\", Win\",\"{\"\"total\"\":118}\""));
        assertTrue(lineas[2].startsWith(",UPDATE,preformas_venta,"));
        assertTrue(lineas[2].contains(",SISTEMA,"));
        assertEquals(3, lineas.length);
        assertTrue(archivo.nombreOriginal().matches("auditoria_\\d{8}_\\d{4}\\.csv"));
        verify(auditService).registrar(eq("audit_log"), eq(admin.getId()), eq(AccionAudit.ACCESS),
                argThat(m -> Long.valueOf(2).equals(m.get("filas"))));
    }

    @Test
    void exportar_recorreTodasLasPaginas() {
        AuditLog a = AuditLog.builder().id(UUID.randomUUID()).tabla("ventas").accion(AccionAudit.INSERT).build();
        PageRequest p0 = PageRequest.of(0, 1000), p1 = PageRequest.of(1, 1000);
        when(auditLogRepository.buscarParaExportar(any(), any(), any(), any(), any(), any(), anyBoolean(), eq(p0)))
                .thenReturn(new PageImpl<>(List.of(a), p0, 1001));
        when(auditLogRepository.buscarParaExportar(any(), any(), any(), any(), any(), any(), anyBoolean(), eq(p1)))
                .thenReturn(new PageImpl<>(List.of(a), p1, 1001));

        service.exportarCsv("ventas", null, null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 8));

        verify(auditLogRepository, times(2)).buscarParaExportar(eq("ventas"), isNull(), isNull(), isNull(),
                eq(LocalDateTime.of(2026, 10, 1, 0, 0)), eq(LocalDateTime.of(2026, 10, 9, 0, 0)), eq(false), any());
    }

    @Test
    void exportar_superaElMaximo_lanzaIllegalArgumentSinAuditar() {
        when(auditLogRepository.buscarParaExportar(any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1000), ParametrosReportesDTO.DEFAULTS.maxFilasExportacion() + 1L));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.exportarCsv(null, null, null, null, null));
        assertTrue(ex.getMessage().contains("50000"));
        verifyNoInteractions(auditService);
    }

    @Test
    void celda_escapaYEvitaInyeccionDeFormulas() {
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", AuditoriaServiceImpl.celda("=HYPERLINK(\"x\")"));
        assertEquals("'-5", AuditoriaServiceImpl.celda("-5"));
        assertEquals("'@SUM(A1)", AuditoriaServiceImpl.celda("@SUM(A1)"));
        assertEquals("\"a,b\"", AuditoriaServiceImpl.celda("a,b"));
        assertEquals("\"linea1\nlinea2\"", AuditoriaServiceImpl.celda("linea1\nlinea2"));
        assertEquals("", AuditoriaServiceImpl.celda(null));
        assertEquals("ventas", AuditoriaServiceImpl.celda("ventas"));
    }

    // ===================== DASHBOARD =====================

    @Test
    void dashboard_rellenaDiasSinEventosYTotaliza() {
        LocalDate d = LocalDate.of(2026, 10, 1), h = LocalDate.of(2026, 10, 3);
        when(auditLogRepository.conteoPorAccion(any(), any())).thenReturn(List.of(accion(AccionAudit.INSERT, 5), accion(AccionAudit.UPDATE, 2)));
        when(auditLogRepository.conteoPorDia(any(), any())).thenReturn(List.of(dia(LocalDate.of(2026, 10, 1), 4), dia(LocalDate.of(2026, 10, 3), 3)));
        when(auditLogRepository.topUsuarios(any(), any(), eq(PageRequest.of(0, 10)))).thenReturn(List.of(usuario("Ana", "Ruiz", 6)));
        when(auditLogRepository.topTablas(any(), any(), any())).thenReturn(List.of(tabla("ventas", 7)));
        when(auditLogRepository.contarSinUsuario(any(), any())).thenReturn(1L);
        when(auditLogRepository.countByCreatedAtIsNull()).thenReturn(40L);

        AuditoriaDashboardDTO r = service.dashboard(d, h);

        assertEquals(7, r.totalEventos());
        assertEquals(0L, r.eventosPorAccion().get("DELETE"));
        assertEquals(5L, r.eventosPorAccion().get("INSERT"));
        assertEquals(List.of(4L, 0L, 3L), r.eventosPorDia().stream().map(c -> c.cantidad()).toList());
        assertEquals("Ana Ruiz", r.topUsuarios().get(0).nombre());
        assertEquals(1, r.eventosDelSistema());
        assertEquals(40, r.logsSinFecha());
        verify(auditLogRepository).conteoPorAccion(LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 4, 0, 0));
    }

    @Test
    void dashboard_sinFechasUltimos30DiasYRangoMaximo() {
        when(auditLogRepository.conteoPorAccion(any(), any())).thenReturn(List.of());
        when(auditLogRepository.conteoPorDia(any(), any())).thenReturn(List.of());
        when(auditLogRepository.topUsuarios(any(), any(), any())).thenReturn(List.of());
        when(auditLogRepository.topTablas(any(), any(), any())).thenReturn(List.of());

        AuditoriaDashboardDTO r = service.dashboard(null, null);

        assertEquals(30, r.eventosPorDia().size());
        assertEquals(LocalDate.now(), r.hasta());
        assertThrows(IllegalArgumentException.class, () -> service.dashboard(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2)));
    }

    // ===================== HELPERS =====================

    private ConteoPorAccionProjection accion(AccionAudit a, long n) {
        return new ConteoPorAccionProjection() {
            public AccionAudit getAccion() { return a; }
            public Long getCantidad() { return n; }
        };
    }

    private ConteoPorFechaProjection dia(LocalDate f, long n) {
        return new ConteoPorFechaProjection() {
            public LocalDate getFecha() { return f; }
            public Long getCantidad() { return n; }
        };
    }

    private ActividadUsuarioProjection usuario(String nombre, String apellido, long n) {
        return new ActividadUsuarioProjection() {
            public UUID getUserId() { return UUID.randomUUID(); }
            public String getNombre() { return nombre; }
            public String getApellido() { return apellido; }
            public String getEmail() { return "x@policalum.com"; }
            public Long getCantidad() { return n; }
        };
    }

    private ConteoPorTablaProjection tabla(String t, long n) {
        return new ConteoPorTablaProjection() {
            public String getTabla() { return t; }
            public Long getCantidad() { return n; }
        };
    }
}
