package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.RolRequestDTO;
import com.policalum.erp.application.dto.response.RolResponseDTO;
import com.policalum.erp.application.mapper.RolMapper;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.infrastructure.repository.RolRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** Roles y permisos. */
@ExtendWith(MockitoExtension.class)
class RolImplTest {

    @Mock RolRepository rolRepository;
    @Spy RolMapper rolMapper = Mappers.getMapper(RolMapper.class);
    @InjectMocks RolImpl service;

    UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(rolRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Rol existente() {
        Rol r = Rol.builder().id(id).nombre(RolNombre.ROLE_MULTIFUNCIONAL).permisosJson(new HashMap<>(Map.of("ventas", true))).build();
        lenient().when(rolRepository.findById(id)).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void createRol_guardaNombreYPermisos() {
        service.createRol(new RolRequestDTO(RolNombre.ROLE_ADMIN, Map.of("todo", true)));

        ArgumentCaptor<Rol> c = ArgumentCaptor.forClass(Rol.class);
        verify(rolRepository).save(c.capture());
        assertEquals(RolNombre.ROLE_ADMIN, c.getValue().getNombre());
        assertEquals(Map.of("todo", true), c.getValue().getPermisosJson());
    }

    @Test
    void updateRol_reemplazaPermisos() {
        Rol r = existente();

        service.updateRol(id, new RolRequestDTO(RolNombre.ROLE_MULTIFUNCIONAL, Map.of("ventas", false, "caja", true)));

        assertEquals(Map.of("ventas", false, "caja", true), r.getPermisosJson());
        verify(rolRepository).save(r);
    }

    @Test
    void updateRol_inexistente_lanzaSinGuardar() {
        when(rolRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateRol(id, new RolRequestDTO(RolNombre.ROLE_ADMIN, Map.of("x", 1))));
        assertTrue(ex.getMessage().contains(id.toString()));
        verify(rolRepository, never()).save(any());
    }

    @Test
    void getRolById_existente_mapea() {
        existente();

        RolResponseDTO r = service.getRolById(id);

        assertNotNull(r);
        verify(rolMapper).toResponseDto(any(Rol.class));
    }

    @Test
    void getRolById_inexistente_lanza() {
        when(rolRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getRolById(id));
    }

    @Test
    void getRoles_mapeaTodos() {
        when(rolRepository.findAll()).thenReturn(List.of(Rol.builder().nombre(RolNombre.ROLE_ADMIN).build(),
                Rol.builder().nombre(RolNombre.ROLE_MULTIFUNCIONAL).build()));

        assertEquals(2, service.getRoles().size());
    }

    @Test
    void deleteRol_existente_borra() {
        when(rolRepository.existsById(id)).thenReturn(true);

        service.deleteRol(id);

        verify(rolRepository).deleteById(id);
    }

    @Test
    void deleteRol_inexistente_lanzaSinBorrar() {
        when(rolRepository.existsById(id)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> service.deleteRol(id));
        verify(rolRepository, never()).deleteById(any());
    }
}
