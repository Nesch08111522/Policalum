package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.response.CompraAdjuntoResponseDTO;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.model.Compra;
import com.policalum.erp.domain.model.CompraAdjunto;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.FileStorageException;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.config.FileStorageProperties;
import com.policalum.erp.infrastructure.repository.CompraAdjuntoRepository;
import com.policalum.erp.infrastructure.repository.CompraRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import jakarta.validation.ValidationException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Módulo 1: adjuntos de compra (factura, guía, OC) con validación de tipo y pertenencia. */
@ExtendWith(MockitoExtension.class)
class CompraAdjuntoServiceImplTest {

    @Mock CompraAdjuntoRepository adjuntoRepository;
    @Mock CompraRepository compraRepository;
    @Mock UserRepository userRepository;
    @Mock FileStorageService fileStorageService;
    @Spy FileStorageProperties props = new FileStorageProperties();
    @Mock AuditService auditService;
    @InjectMocks CompraAdjuntoServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    User admin;
    Compra compra;
    MockMultipartFile pdf;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(UUID.randomUUID()).nombre("Miguel").apellido("Dueño").email(EMAIL).build();
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenReturn(Optional.of(admin));

        compra = new Compra();
        compra.setId(UUID.randomUUID());
        lenient().when(compraRepository.findById(compra.getId())).thenReturn(Optional.of(compra));
        lenient().when(adjuntoRepository.save(any())).thenAnswer(i -> {
            CompraAdjunto a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        pdf = new MockMultipartFile("file", "factura.pdf", "application/pdf", new byte[] {1, 2, 3});
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CompraAdjunto adjunto(Compra de) {
        return CompraAdjunto.builder().id(UUID.randomUUID()).compra(de).tipo("FACTURA")
                .nombreOriginal("factura.pdf").urlArchivo("compras/2026/10/x.pdf").build();
    }

    // ---------------------------------------------------------------- subir

    @Test
    void subir_tipoEnMinusculasConEspacios_seNormalizaYGuardaMetadatos() {
        when(fileStorageService.guardar(pdf, "compras")).thenReturn("compras/2026/10/abc.pdf");

        CompraAdjuntoResponseDTO r = service.subir(compra.getId(), pdf, "  factura ");

        assertEquals("FACTURA", r.tipo());
        assertEquals("factura.pdf", r.nombreOriginal());
        assertEquals("compras/2026/10/abc.pdf", r.urlArchivo());
        assertEquals("application/pdf", r.contentType());
        assertEquals(3L, r.tamanoBytes());
        assertEquals("Miguel Dueño", r.subidoPorNombre());
        verify(auditService).registrar(eq("compra_adjuntos"), eq(r.id()), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void subir_tipoNulo_usaOtro() {
        when(fileStorageService.guardar(any(), any())).thenReturn("compras/x.pdf");

        assertEquals("OTRO", service.subir(compra.getId(), pdf, null).tipo());
    }

    @Test
    void subir_tipoNoPermitido_lanzaValidationSinGuardarArchivo() {
        assertThrows(ValidationException.class, () -> service.subir(compra.getId(), pdf, "CONTRATO"));
        verifyNoInteractions(fileStorageService, adjuntoRepository);
    }

    @Test
    void subir_compraInexistente_lanzaNotFoundSinGuardarArchivo() {
        UUID otra = UUID.randomUUID();
        when(compraRepository.findById(otra)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.subir(otra, pdf, "FACTURA"));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void subir_errorDeAlmacenamiento_propagaSinRegistrarEnBd() {
        when(fileStorageService.guardar(any(), any())).thenThrow(new FileStorageException("Extensión no permitida"));

        assertThrows(FileStorageException.class, () -> service.subir(compra.getId(), pdf, "FACTURA"));
        verify(adjuntoRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- listar / descargar

    @Test
    void listar_mapeaSinUsuarioCuandoNoHaySubidoPor() {
        when(adjuntoRepository.findByCompra_IdOrderByCreatedAtDesc(compra.getId())).thenReturn(List.of(adjunto(compra)));

        List<CompraAdjuntoResponseDTO> r = service.listar(compra.getId());

        assertEquals(1, r.size());
        assertNull(r.get(0).subidoPorId());
        assertNull(r.get(0).subidoPorNombre());
    }

    @Test
    void descargar_adjuntoDeLaCompra_cargaDesdeAlmacenamiento() {
        CompraAdjunto a = adjunto(compra);
        Resource res = new ByteArrayResource(new byte[0]);
        when(adjuntoRepository.findById(a.getId())).thenReturn(Optional.of(a));
        when(fileStorageService.cargar(a.getUrlArchivo())).thenReturn(res);

        assertSame(res, service.descargar(compra.getId(), a.getId()));
    }

    @Test
    void descargar_adjuntoDeOtraCompra_lanzaFileStorageExceptionSinLeerDisco() {
        Compra otra = new Compra();
        otra.setId(UUID.randomUUID());
        CompraAdjunto a = adjunto(otra);
        when(adjuntoRepository.findById(a.getId())).thenReturn(Optional.of(a));

        assertThrows(FileStorageException.class, () -> service.descargar(compra.getId(), a.getId()));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void descargar_adjuntoInexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(adjuntoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.descargar(compra.getId(), id));
    }

    // ---------------------------------------------------------------- eliminar

    @Test
    void eliminar_borraArchivoRegistroYAudita() {
        CompraAdjunto a = adjunto(compra);
        when(adjuntoRepository.findById(a.getId())).thenReturn(Optional.of(a));

        service.eliminar(compra.getId(), a.getId());

        verify(fileStorageService).eliminar(a.getUrlArchivo());
        verify(adjuntoRepository).delete(a);
        verify(auditService).registrar(eq("compra_adjuntos"), eq(a.getId()), eq(AccionAudit.DELETE), anyMap());
    }

    @Test
    void eliminar_fallaElBorradoFisico_igualEliminaElRegistro() {
        CompraAdjunto a = adjunto(compra);
        when(adjuntoRepository.findById(a.getId())).thenReturn(Optional.of(a));
        doThrow(new RuntimeException("disco lleno")).when(fileStorageService).eliminar(any());

        service.eliminar(compra.getId(), a.getId());

        verify(adjuntoRepository).delete(a);
    }

    @Test
    void eliminar_adjuntoDeOtraCompra_lanzaFileStorageExceptionSinBorrar() {
        Compra otra = new Compra();
        otra.setId(UUID.randomUUID());
        CompraAdjunto a = adjunto(otra);
        when(adjuntoRepository.findById(a.getId())).thenReturn(Optional.of(a));

        assertThrows(FileStorageException.class, () -> service.eliminar(compra.getId(), a.getId()));
        verify(adjuntoRepository, never()).delete(any());
        verifyNoInteractions(fileStorageService);
    }
}
