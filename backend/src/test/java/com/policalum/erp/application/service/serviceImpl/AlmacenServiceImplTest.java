package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.AlmacenRequestDTO;
import com.policalum.erp.application.mapper.AlmacenMapper;
import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.AlmacenRepository;
import jakarta.validation.ValidationException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

/** Almacenes: nombre único dentro de cada sede. */
@ExtendWith(MockitoExtension.class)
class AlmacenServiceImplTest {

    @Mock AlmacenRepository almacenRepository;
    @Mock AlmacenMapper almacenMapper;
    @InjectMocks AlmacenServiceImpl service;

    Sede sede;
    Almacen principal;

    @BeforeEach
    void setUp() {
        sede = Sede.builder().id(UUID.randomUUID()).build();
        principal = Almacen.builder().id(UUID.randomUUID()).nombre("Principal").sede(sede).build();
        lenient().when(almacenRepository.findWithSedeById(principal.getId())).thenReturn(Optional.of(principal));
        lenient().when(almacenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private AlmacenRequestDTO request(String nombre) {
        return new AlmacenRequestDTO(sede, nombre, true, null);
    }

    @Test
    void create_nombreLibre_guardaRecortado() {
        Almacen mapeado = new Almacen();
        when(almacenMapper.toEntity(any())).thenReturn(mapeado);
        when(almacenRepository.findBySede_IdAndNombreIgnoreCase(sede.getId(), "Tienda")).thenReturn(Optional.empty());

        service.create(request("  Tienda "));

        assertEquals("Tienda", mapeado.getNombre());
        verify(almacenRepository).save(mapeado);
    }

    @Test
    void create_nombreRepetidoEnLaSede_lanzaValidation() {
        when(almacenRepository.findBySede_IdAndNombreIgnoreCase(sede.getId(), "principal")).thenReturn(Optional.of(principal));

        assertThrows(ValidationException.class, () -> service.create(request("principal")));
        verify(almacenRepository, never()).save(any());
    }

    @Test
    void update_mismoNombre_permitido() {
        when(almacenRepository.findBySede_IdAndNombreIgnoreCase(sede.getId(), "Principal")).thenReturn(Optional.of(principal));

        service.update(principal.getId(), request("Principal "));

        verify(almacenMapper).updateEntityFromDto(any(), eq(principal));
        assertEquals("Principal", principal.getNombre());
    }

    @Test
    void update_nombreDeOtroAlmacenDeLaSede_lanzaValidation() {
        Almacen otro = Almacen.builder().id(UUID.randomUUID()).build();
        when(almacenRepository.findBySede_IdAndNombreIgnoreCase(sede.getId(), "Tienda")).thenReturn(Optional.of(otro));

        assertThrows(ValidationException.class, () -> service.update(principal.getId(), request("Tienda")));
    }

    @Test
    void update_inexistente_lanzaNotFound() {
        UUID x = UUID.randomUUID();
        when(almacenRepository.findWithSedeById(x)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(x, request("A")));
    }

    @Test
    void search_terminoEnBlancoSeEnviaNulo() {
        Pageable pg = PageRequest.of(0, 10);
        when(almacenRepository.search(null, sede.getId(), true, pg)).thenReturn(Page.empty());

        service.search("   ", sede.getId(), true, pg);

        verify(almacenRepository).search(null, sede.getId(), true, pg);
    }

    @Test
    void findBySede_delete_yFindById() {
        when(almacenRepository.findBySede_IdOrderByNombreAsc(sede.getId())).thenReturn(List.of(principal));

        assertEquals(1, service.findBySede(sede.getId()).size());
        service.findById(principal.getId());
        service.delete(principal.getId());

        verify(almacenMapper, times(2)).toResponseDto(principal);
        verify(almacenRepository).delete(principal);
    }

    @Test
    void getDashboardStatistics_cuentaActivos() {
        when(almacenRepository.count()).thenReturn(3L);
        when(almacenRepository.countByActivo(true)).thenReturn(2L);
        when(almacenRepository.countByActivo(false)).thenReturn(1L);

        assertEquals(Map.of("totalAlmacenes", 3L, "almacenesActivos", 2L, "almacenesInactivos", 1L), service.getDashboardStatistics());
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void isNombreDisponible_recortaYDistingueElPropio() {
        when(almacenRepository.findBySede_IdAndNombreIgnoreCase(sede.getId(), "Principal")).thenReturn(Optional.of(principal));
        when(almacenRepository.findBySede_IdAndNombreIgnoreCase(sede.getId(), "Libre")).thenReturn(Optional.empty());

        assertTrue(service.isNombreDisponible(sede.getId(), " Libre ", null));
        assertTrue(service.isNombreDisponible(sede.getId(), "Principal", principal.getId()));
        assertFalse(service.isNombreDisponible(sede.getId(), "Principal", null));
        assertFalse(service.isNombreDisponible(sede.getId(), "Principal", UUID.randomUUID()));
    }
}
