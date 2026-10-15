package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.ServicioRequestDTO;
import com.policalum.erp.application.mapper.ServicioMapper;
import com.policalum.erp.domain.model.Servicio;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.ServicioRepository;
import jakarta.validation.ValidationException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

/** Servicios (corte, instalación): nombre único y baja lógica. */
@ExtendWith(MockitoExtension.class)
class ServicioServiceImplTest {

    @Mock ServicioRepository servicioRepository;
    @Mock ServicioMapper servicioMapper;
    @InjectMocks ServicioServiceImpl service;

    Servicio corte;

    @BeforeEach
    void setUp() {
        corte = Servicio.builder().id(UUID.randomUUID()).nombre("Corte").precio(new BigDecimal("10.00")).activo(true).build();
        lenient().when(servicioRepository.findById(corte.getId())).thenReturn(Optional.of(corte));
        lenient().when(servicioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private ServicioRequestDTO request(String nombre, String precio, boolean activo) {
        return new ServicioRequestDTO(nombre, "desc", new BigDecimal(precio), activo);
    }

    @Test
    void create_nombreLibre_guarda() {
        when(servicioRepository.existsByNombreIgnoreCase("Instalación")).thenReturn(false);
        when(servicioMapper.toEntity(any())).thenReturn(new Servicio());

        service.create(request(" Instalación ", "50.00", true));

        verify(servicioRepository).save(any());
    }

    @Test
    void create_nombreExistente_lanzaValidation() {
        when(servicioRepository.existsByNombreIgnoreCase("corte")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.create(request("corte", "1.00", true)));
        verify(servicioRepository, never()).save(any());
    }

    @Test
    void update_aplicaTodosLosCampos() {
        when(servicioRepository.existsByNombreIgnoreCaseAndIdNot("Corte recto", corte.getId())).thenReturn(false);

        service.update(corte.getId(), request("Corte recto", "12.50", false));

        assertEquals("Corte recto", corte.getNombre());
        assertEquals(new BigDecimal("12.50"), corte.getPrecio());
        assertFalse(corte.getActivo());
        assertEquals("desc", corte.getDescripcion());
    }

    @Test
    void update_nombreDeOtroServicio_lanzaValidationSinCambiar() {
        when(servicioRepository.existsByNombreIgnoreCaseAndIdNot("Instalación", corte.getId())).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.update(corte.getId(), request("Instalación", "1.00", true)));
        assertEquals("Corte", corte.getNombre());
    }

    @Test
    void update_inexistente_lanzaNotFound() {
        UUID x = UUID.randomUUID();
        when(servicioRepository.findById(x)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(x, request("a", "1.00", true)));
    }

    @Test
    void delete_esBajaLogicaNoBorraFisicamente() {
        service.delete(corte.getId());

        assertFalse(corte.getActivo());
        verify(servicioRepository).save(corte);
        verify(servicioRepository, never()).delete(any());
    }

    @Test
    void delete_inexistente_lanzaNotFound() {
        UUID x = UUID.randomUUID();
        when(servicioRepository.findById(x)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(x));
    }

    @Test
    void isNombreDisponible_nuloOVacio_trueSinConsultar() {
        assertTrue(service.isNombreDisponible(null, null));
        assertTrue(service.isNombreDisponible(" ", UUID.randomUUID()));
        verifyNoInteractions(servicioRepository);
    }

    @Test
    void findById_inexistente_lanzaNotFound() {
        UUID x = UUID.randomUUID();
        when(servicioRepository.findById(x)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(x));
    }

    @Test
    void findById_existente_mapea() {
        when(servicioMapper.toResponseDto(corte)).thenReturn(mock(com.policalum.erp.application.dto.response.ServicioResponseDTO.class));

        assertNotNull(service.findById(corte.getId()));
    }

    @Test
    void search_delegaFiltros() {
        Pageable pg = PageRequest.of(0, 10);
        when(servicioRepository.search("cor", true, BigDecimal.ONE, BigDecimal.TEN, pg)).thenReturn(new PageImpl<>(List.of(corte)));

        assertEquals(1, service.search("cor", true, BigDecimal.ONE, BigDecimal.TEN, pg).getTotalElements());
    }

    @Test
    void getDashboardStatistics_inactivosEsTotalMenosActivos() {
        when(servicioRepository.count()).thenReturn(7L);
        when(servicioRepository.countByActivoTrue()).thenReturn(5L);

        Map<String, Object> d = service.getDashboardStatistics();

        assertEquals(7L, d.get("totalServicios"));
        assertEquals(5L, d.get("serviciosActivos"));
        assertEquals(2L, d.get("serviciosInactivos"));
    }
}
