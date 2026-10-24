package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.policalum.erp.application.dto.request.PerfilActualizarRequestDTO;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.application.service.TotpService;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.RrhhLegajoDigitalRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.security.JwtUtil;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PerfilServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock SedeRepository sedeRepository;
    @Mock RrhhLegajoDigitalRepository legajoRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock TotpService totpService;
    @Mock FileStorageService fileStorageService;

    PerfilServiceImpl service;
    User usuario;
    UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PerfilServiceImpl(userRepository, sedeRepository, legajoRepository, passwordEncoder, jwtUtil, totpService, fileStorageService);
        usuario = User.builder().id(id).nombre("Ana").apellido("Perez").nombreUsuario("ana").email("ana@policalum.com")
                .activo(true).rol(Rol.builder().nombre(RolNombre.ROLE_ADMIN).build())
                .sede(Sede.builder().id(UUID.randomUUID()).nombre("Principal").build()).build();
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("ana@policalum.com");
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findPerfilByEmail("ana@policalum.com")).thenReturn(Optional.of(usuario));
        lenient().when(legajoRepository.findDetalleByUserId(id)).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void guardarFoto_dejaLaRutaYNoElContenido() {
        when(fileStorageService.guardarFijo(any(), org.mockito.ArgumentMatchers.eq("usuarios/" + id + "/foto")))
                .thenReturn("usuarios/" + id + "/foto.png");
        when(userRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        MockMultipartFile archivo = new MockMultipartFile("archivo", "retrato.png", "image/png", new byte[]{1, 2, 3});

        var respuesta = service.guardarFoto(archivo);

        assertEquals("usuarios/" + id + "/foto.png", respuesta.foto());
        verify(fileStorageService).descartarVariantes("usuarios/" + id + "/foto.png");
    }

    @Test
    void actualizar_rechazaUnDataUrl() {
        when(userRepository.findByEmail("ana@policalum.com")).thenReturn(Optional.empty());
        PerfilActualizarRequestDTO request = new PerfilActualizarRequestDTO(
                "Ana", "Perez", "ana@policalum.com", "987654321", "", "",
                "data:image/png;base64,aaaa", null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.actualizar(request));
    }
}
