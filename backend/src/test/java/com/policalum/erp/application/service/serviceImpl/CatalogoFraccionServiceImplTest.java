package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.CatalogoFraccionRequestDTO;
import com.policalum.erp.application.mapper.CatalogoFraccionMapper;
import com.policalum.erp.domain.model.CatalogoFraccion;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CatalogoFraccionRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;
import jakarta.validation.ValidationException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

/** Fracciones de plancha: solo para productos fraccionables, factor en (0, 1] y único por producto. */
@ExtendWith(MockitoExtension.class)
class CatalogoFraccionServiceImplTest {

    @Mock CatalogoFraccionRepository catalogoFraccionRepository;
    @Mock CatalogoFraccionMapper catalogoFraccionMapper;
    @Mock ProductoRepository productoRepository;
    @InjectMocks CatalogoFraccionServiceImpl service;

    Producto plancha;
    CatalogoFraccion media;

    @BeforeEach
    void setUp() {
        plancha = Producto.builder().id(UUID.randomUUID()).esFraccionable(true).build();
        media = CatalogoFraccion.builder().id(UUID.randomUUID()).producto(plancha).nombreFraccion("Media")
                .factorDescuentoKardex(new BigDecimal("0.5000")).build();
        lenient().when(productoRepository.findById(plancha.getId())).thenReturn(Optional.of(plancha));
        lenient().when(productoRepository.existsByIdAndEsFraccionableTrue(plancha.getId())).thenReturn(true);
        lenient().when(catalogoFraccionRepository.findByProducto_IdAndFactorDescuentoKardex(eq(plancha.getId()), any())).thenReturn(Optional.empty());
        lenient().when(catalogoFraccionRepository.findWithProductoById(media.getId())).thenReturn(Optional.of(media));
        lenient().when(catalogoFraccionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private CatalogoFraccionRequestDTO request(String nombre, String factor) {
        return new CatalogoFraccionRequestDTO(plancha.getId(), nombre, new BigDecimal(factor), null, true);
    }

    @Test
    void create_valido_asignaProductoYNormaliza() {
        CatalogoFraccion mapeada = CatalogoFraccion.builder().nombreFraccion("  Tercio ").factorDescuentoKardex(new BigDecimal("0.3300")).build();
        when(catalogoFraccionMapper.toEntity(any())).thenReturn(mapeada);

        service.create(request("  Tercio ", "0.3300"));

        assertSame(plancha, mapeada.getProducto());
        assertEquals("Tercio", mapeada.getNombreFraccion());
        assertEquals("0.33", mapeada.getFactorDescuentoKardex().toPlainString(), "se quitan ceros a la derecha");
    }

    @Test
    void create_factorUno_esValido() {
        when(catalogoFraccionMapper.toEntity(any())).thenReturn(CatalogoFraccion.builder().nombreFraccion("Entera").factorDescuentoKardex(BigDecimal.ONE).build());

        assertDoesNotThrow(() -> service.create(request("Entera", "1")));
    }

    @Test
    void create_factorMayorQueUno_lanzaValidation() {
        assertThrows(ValidationException.class, () -> service.create(request("Doble", "1.0001")));
        verify(catalogoFraccionRepository, never()).save(any());
    }

    @Test
    void create_factorCero_lanzaValidation() {
        assertThrows(ValidationException.class, () -> service.create(request("Nada", "0")));
    }

    @Test
    void create_productoNoFraccionable_lanzaValidation() {
        when(productoRepository.existsByIdAndEsFraccionableTrue(plancha.getId())).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.create(request("Media", "0.5")));
        assertTrue(ex.getMessage().contains("fraccionables"));
    }

    @Test
    void create_productoInexistente_lanzaNotFound() {
        UUID x = UUID.randomUUID();
        when(productoRepository.findById(x)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(new CatalogoFraccionRequestDTO(x, "a", BigDecimal.ONE, null, true)));
    }

    @Test
    void create_factorRepetidoEnElProducto_lanzaValidation() {
        when(catalogoFraccionRepository.findByProducto_IdAndFactorDescuentoKardex(plancha.getId(), new BigDecimal("0.5"))).thenReturn(Optional.of(media));

        assertThrows(ValidationException.class, () -> service.create(request("Otra media", "0.5")));
    }

    @Test
    void update_mismoFactorDeLaMismaFraccion_permitido() {
        when(catalogoFraccionRepository.findByProducto_IdAndFactorDescuentoKardex(plancha.getId(), new BigDecimal("0.5"))).thenReturn(Optional.of(media));

        service.update(media.getId(), request(" Media plancha ", "0.5"));

        verify(catalogoFraccionMapper).updateEntityFromDto(any(), eq(media));
        verify(catalogoFraccionRepository).save(media);
    }

    @Test
    void update_inexistente_lanzaNotFound() {
        UUID x = UUID.randomUUID();
        when(catalogoFraccionRepository.findWithProductoById(x)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(x, request("a", "0.5")));
    }

    @Test
    void findAvailableByProducto_soloActivasOrdenadas() {
        when(catalogoFraccionRepository.findByProducto_IdAndActivoTrueOrderByFactorDescuentoKardexDesc(plancha.getId())).thenReturn(List.of(media));

        assertEquals(1, service.findAvailableByProducto(plancha.getId()).size());
    }

    @Test
    void search_terminoEnBlancoSeEnviaNulo() {
        Pageable pg = PageRequest.of(0, 10);
        when(catalogoFraccionRepository.search(plancha.getId(), true, null, pg)).thenReturn(Page.empty());

        service.search(plancha.getId(), true, "  ", pg);

        verify(catalogoFraccionRepository).search(plancha.getId(), true, null, pg);
    }

    @Test
    void delete_yFindById_inexistente() {
        service.delete(media.getId());
        verify(catalogoFraccionRepository).delete(media);

        UUID x = UUID.randomUUID();
        when(catalogoFraccionRepository.findWithProductoById(x)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(x));
    }

    @Test
    void getDashboardStatistics_cuentaActivas() {
        when(catalogoFraccionRepository.count()).thenReturn(10L);
        when(catalogoFraccionRepository.countByActivo(true)).thenReturn(9L);
        when(catalogoFraccionRepository.countByActivo(false)).thenReturn(1L);

        assertEquals(Map.of("totalFracciones", 10L, "fraccionesActivas", 9L, "fraccionesInactivas", 1L), service.getDashboardStatistics());
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void isFactorDisponible_casos() {
        when(catalogoFraccionRepository.findByProducto_IdAndFactorDescuentoKardex(plancha.getId(), new BigDecimal("0.5"))).thenReturn(Optional.of(media));

        assertTrue(service.isFactorDisponible(plancha.getId(), new BigDecimal("0.25"), null));
        assertTrue(service.isFactorDisponible(plancha.getId(), new BigDecimal("0.5"), media.getId()));
        assertFalse(service.isFactorDisponible(plancha.getId(), new BigDecimal("0.5"), null));
    }
}
