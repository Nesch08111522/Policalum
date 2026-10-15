package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.model.AuditLog;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.AuditLogRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Bitácora: registra quién/qué/cuándo y nunca rompe la operación de negocio. */
@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock UserRepository userRepository;
    @InjectMocks AuditServiceImpl service;

    User admin;
    UUID registro = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        admin = User.builder().id(UUID.randomUUID()).nombre("Miguel").apellido("Dueño").email("admin@policalum.com").build();
        lenient().when(userRepository.findByEmailAndActivoTrue("admin@policalum.com")).thenReturn(Optional.of(admin));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private AuditLog guardado() {
        ArgumentCaptor<AuditLog> c = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(c.capture());
        return c.getValue();
    }

    private void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@policalum.com", "x", AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @Test
    void registrar_conUsuarioAutenticadoYRequest_guardaUsuarioYUserAgent() {
        autenticar();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("User-Agent", "Mozilla/5.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        service.registrar("ventas", registro, AccionAudit.UPDATE, Map.of("total", 10));

        AuditLog a = guardado();
        assertSame(admin, a.getUser());
        assertEquals("Mozilla/5.0", a.getUserAgent());
        assertEquals("ventas", a.getTabla());
        assertEquals(registro, a.getRegistroId());
        assertEquals(AccionAudit.UPDATE, a.getAccion());
        assertEquals(Map.of("total", 10), a.getDatosJson());
    }

    @Test
    void registrar_userAgentMuyLargo_seTruncaA1000() {
        autenticar();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("User-Agent", "x".repeat(1500));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        service.registrar("ventas", registro, AccionAudit.INSERT, Map.of());

        assertEquals(1000, guardado().getUserAgent().length());
    }

    @Test
    void registrar_sinAutenticacionNiRequest_guardaComoSistema() {
        service.registrar("preformas", registro, AccionAudit.UPDATE, Map.of());

        AuditLog a = guardado();
        assertNull(a.getUser());
        assertNull(a.getUserAgent());
        verifyNoInteractions(userRepository);
    }

    @Test
    void registrar_usuarioAnonimo_guardaSinUsuario() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("k", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        service.registrar("auth", registro, AccionAudit.ACCESS, Map.of());

        assertNull(guardado().getUser());
        verifyNoInteractions(userRepository);
    }

    @Test
    void registrar_errorAlGuardar_noPropagaParaNoRomperLaOperacion() {
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("BD caída"));

        assertDoesNotThrow(() -> service.registrar("ventas", registro, AccionAudit.DELETE, null, null, Map.of()));
    }

    @Test
    void search_mapeaFilasConYSinUsuario() {
        Pageable pg = PageRequest.of(0, 10);
        AuditLog conUsuario = AuditLog.builder().id(UUID.randomUUID()).tabla("ventas").accion(AccionAudit.INSERT).user(admin).build();
        AuditLog sistema = AuditLog.builder().id(UUID.randomUUID()).tabla("preformas").accion(AccionAudit.UPDATE).build();
        when(auditLogRepository.search("ventas", null, null, null, null, null, pg)).thenReturn(new PageImpl<>(List.of(conUsuario, sistema)));

        List<Map<String, Object>> filas = service.search("ventas", null, null, null, null, null, pg).getContent();

        assertEquals("Miguel Dueño", filas.get(0).get("userNombre"));
        assertEquals(admin.getId(), filas.get(0).get("userId"));
        assertNull(filas.get(1).get("userId"));
        assertNull(filas.get(1).get("userNombre"));
        assertEquals(AccionAudit.UPDATE, filas.get(1).get("accion"));
    }
}
