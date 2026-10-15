package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.UserRequestDTO;
import com.policalum.erp.application.dto.response.UserResponseDTO;
import com.policalum.erp.application.mapper.UserMapper;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Usuarios: la contraseña nunca se guarda en claro y una edición sin contraseña conserva la actual. */
@ExtendWith(MockitoExtension.class)
class UserImplTest {

    @Mock PasswordEncoder passwordEncoder;
    @Mock UserRepository userRepository;
    @Spy UserMapper userMapper = Mappers.getMapper(UserMapper.class);
    @InjectMocks UserImpl service;

    Rol rol;
    Sede sede;

    @BeforeEach
    void setUp() {
        rol = Rol.builder().id(UUID.randomUUID()).nombre(RolNombre.ROLE_MULTIFUNCIONAL).build();
        sede = Sede.builder().id(UUID.randomUUID()).nombre("Centro").build();
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(i -> "{bcrypt}" + i.getArgument(0));
        lenient().when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private UserRequestDTO request(String password, Boolean activo) {
        return new UserRequestDTO(rol, sede, "ana_ruiz", "Ana", "Ruiz", "ana@policalum.com", password, "987654321", activo);
    }

    private User existente() {
        User u = User.builder().id(UUID.randomUUID()).nombreUsuario("ana_old").nombre("Ana").apellido("Vieja")
                .email("old@policalum.com").passwordHash("{bcrypt}HashActual1!").activo(true).build();
        lenient().when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        return u;
    }

    // ---------------------------------------------------------------- crear

    @Test
    void createUser_encriptaLaContrasenaYNuncaLaGuardaEnClaro() {
        service.createUser(request("Secreta1!", true));

        ArgumentCaptor<User> c = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(c.capture());
        assertEquals("{bcrypt}Secreta1!", c.getValue().getPasswordHash());
        verify(passwordEncoder).encode("Secreta1!");
    }

    @Test
    void createUser_siempreQuedaActivoAunqueElDtoDigaLoContrario() {
        service.createUser(request("Secreta1!", false));

        ArgumentCaptor<User> c = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(c.capture());
        assertTrue(c.getValue().getActivo());
        assertSame(rol, c.getValue().getRol());
        assertSame(sede, c.getValue().getSede());
    }

    @Test
    void createUser_respuestaNoExponeLaContrasena() {
        UserResponseDTO r = service.createUser(request("Secreta1!", true));

        assertEquals("ana@policalum.com", r.email());
        assertFalse(r.toString().contains("Secreta1!"));
        assertFalse(r.toString().contains("bcrypt"));
    }

    // ---------------------------------------------------------------- actualizar

    @Test
    void updateUser_conNuevaContrasena_laEncripta() {
        User u = existente();

        service.updateUser(u.getId(), request("Nueva123!", true));

        assertEquals("{bcrypt}Nueva123!", u.getPasswordHash());
        assertEquals("ana@policalum.com", u.getEmail());
    }

    @Test
    void updateUser_contrasenaVacia_conservaElHashActual() {
        User u = existente();

        service.updateUser(u.getId(), request("", true));

        assertEquals("{bcrypt}HashActual1!", u.getPasswordHash(), "el mapper copia \"\" pero el servicio restaura el hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUser_contrasenaNula_conservaElHashActual() {
        User u = existente();

        service.updateUser(u.getId(), request(null, true));

        assertEquals("{bcrypt}HashActual1!", u.getPasswordHash());
    }

    @Test
    void updateUser_contrasenaEnBlanco_conservaElHashActual() {
        User u = existente();

        service.updateUser(u.getId(), request("   ", true));

        assertEquals("{bcrypt}HashActual1!", u.getPasswordHash());
    }

    @Test
    void updateUser_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateUser(id, request("x", true)));
        verify(userRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas / borrar

    @Test
    void findById_existente_mapea() {
        User u = existente();

        Optional<UserResponseDTO> r = service.findById(u.getId());

        assertTrue(r.isPresent());
        assertEquals("ana_old", r.get().nombreUsuario());
    }

    @Test
    void findById_inexistente_vacio() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertTrue(service.findById(id).isEmpty());
    }

    @Test
    void getUsers_mapeaTodos() {
        when(userRepository.findAll()).thenReturn(List.of(User.builder().email("a@x.com").build(), User.builder().email("b@x.com").build()));

        assertEquals(List.of("a@x.com", "b@x.com"), service.getUsers().stream().map(UserResponseDTO::email).toList());
    }

    @Test
    void deleteUser_existente_borra() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);

        service.DeleteUser(id);

        verify(userRepository).deleteById(id);
    }

    @Test
    void deleteUser_inexistente_lanzaSinBorrar() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> service.DeleteUser(id));
        verify(userRepository, never()).deleteById(any());
    }

    // ===================== Completado en auditoría de cobertura =====================
    /**
     * RIESGO: UserMapper.updateEntityFromDto copia dto.activo() tal cual. Si el cliente edita un
     * usuario sin enviar "activo", queda activo = null: findByEmailAndActivoTrue deja de encontrarlo
     * (no puede iniciar sesión) y RrhhAsistenciaImpl hace !usuario.getActivo() → NPE.
     * Habilitar tras agregar @BeanMapping(nullValuePropertyMappingStrategy = IGNORE) o equivalente.
     */
    @Test
    @Disabled("Riesgo: editar sin 'activo' deja activo = null. Habilitar tras ignorar nulos en UserMapper.updateEntityFromDto")
    void updateUser_sinCampoActivo_conservaElEstadoActual() {
        User u = existente();

        service.updateUser(u.getId(), request("Nueva123!", null));

        assertEquals(Boolean.TRUE, u.getActivo());
    }
}
