package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.SedeRequestDTO;
import com.policalum.erp.application.mapper.SedeMapper;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import jakarta.validation.ValidationException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

/** Sedes: valores por defecto Perú / PEN / activa. */
@ExtendWith(MockitoExtension.class)
class SedeServiceImplTest {

    @Mock SedeRepository sedeRepository;
    @Mock SedeMapper sedeMapper;
    @Mock FileStorageService fileStorageService;
    @InjectMocks SedeServiceImpl service;

    UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(sedeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private SedeRequestDTO request() {
        return new SedeRequestDTO("Trujillo Centro", null, "Trujillo", "Jr. Pizarro 1", "944111222", null, null, null, null, null);
    }

    @Test
    void crearSede_sinPaisMonedaNiEstado_aplicaDefaultsPeruanos() {
        Sede mapeada = new Sede();
        mapeada.setPais(null);
        mapeada.setMoneda(null);
        mapeada.setActiva(null);
        when(sedeMapper.toEntity(any())).thenReturn(mapeada);

        service.crearSede(request());

        assertEquals("Perú", mapeada.getPais());
        assertEquals("PEN", mapeada.getMoneda());
        assertTrue(mapeada.getActiva());
    }

    @Test
    void crearSede_conValoresExplicitos_losRespeta() {
        Sede mapeada = new Sede();
        mapeada.setPais("Chile");
        mapeada.setMoneda("USD");
        mapeada.setActiva(false);
        when(sedeMapper.toEntity(any())).thenReturn(mapeada);

        service.crearSede(request());

        assertEquals("Chile", mapeada.getPais());
        assertEquals("USD", mapeada.getMoneda());
        assertFalse(mapeada.getActiva());
    }

    @Test
    void actualizarSede_existente_aplicaCambios() {
        Sede s = Sede.builder().id(id).build();
        when(sedeRepository.findById(id)).thenReturn(Optional.of(s));

        service.actualizarSede(id, request());

        verify(sedeMapper).updateEntityFromDto(any(), eq(s));
        verify(sedeRepository).save(s);
    }

    @Test
    void actualizarSede_inexistente_lanzaSinGuardar() {
        when(sedeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSede(id, request()));
        verify(sedeRepository, never()).save(any());
    }

    @Test
    void buscarPorId_inexistente_lanza() {
        when(sedeRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(id));
        assertTrue(ex.getMessage().contains(id.toString()));
    }

    @Test
    void eliminarSede_sinHistorial_borra() {
        Sede sede = Sede.builder().id(id).activa(true).build();
        when(sedeRepository.findById(id)).thenReturn(Optional.of(sede));

        assertNull(service.eliminarSede(id));

        verify(sedeRepository).delete(sede);
    }

    @Test
    void eliminarSede_conHistorial_desactiva() {
        Sede sede = Sede.builder().id(id).activa(true).build();
        when(sedeRepository.findById(id)).thenReturn(Optional.of(sede));
        when(sedeRepository.tieneHistorial(id)).thenReturn(true);

        service.eliminarSede(id);

        assertFalse(sede.getActiva());
        verify(sedeRepository).save(sede);
        verify(sedeRepository, never()).delete(any());
    }

    @Test
    void eliminarSede_inactivaConHistorial_rechaza() {
        Sede sede = Sede.builder().id(id).activa(false).build();
        when(sedeRepository.findById(id)).thenReturn(Optional.of(sede));
        when(sedeRepository.tieneHistorial(id)).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.eliminarSede(id));
        verify(sedeRepository, never()).save(any());
        verify(sedeRepository, never()).delete(any());
    }

    @Test
    void eliminarSede_inexistente_lanzaNotFound() {
        when(sedeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarSede(id));
        verify(sedeRepository, never()).delete(any());
    }

    @Test
    void listarTodasYPorEstado_delegan() {
        Pageable pg = PageRequest.of(0, 10);
        when(sedeRepository.findAll(pg)).thenReturn(new PageImpl<>(List.of(new Sede(), new Sede())));
        when(sedeRepository.findByActiva(true, pg)).thenReturn(new PageImpl<>(List.of(new Sede())));

        assertEquals(2, service.listarTodas(pg).getTotalElements());
        assertEquals(1, service.listarPorEstado(true, pg).getTotalElements());
    }
}
