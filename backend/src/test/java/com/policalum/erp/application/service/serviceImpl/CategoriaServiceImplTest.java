package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.CategoriaRequestDTO;
import com.policalum.erp.application.mapper.CategoriaMapper;
import com.policalum.erp.domain.model.Categoria;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CategoriaRepository;
import jakarta.validation.ValidationException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

/** Categorías jerárquicas: slug único y sin ciclos padre/hijo. */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceImplTest {

    @Mock CategoriaRepository categoriaRepository;
    @Mock CategoriaMapper categoriaMapper;
    @InjectMocks CategoriaServiceImpl service;

    Categoria raiz, hija, nieta;

    @BeforeEach
    void setUp() {
        lenient().when(categoriaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        raiz = Categoria.builder().id(UUID.randomUUID()).nombre("Plásticos").slug("plasticos").build();
        hija = Categoria.builder().id(UUID.randomUUID()).nombre("Policarbonato").slug("policarbonato").parent(raiz).build();
        nieta = Categoria.builder().id(UUID.randomUUID()).nombre("Alveolar").slug("alveolar").parent(hija).build();
        for (Categoria c : List.of(raiz, hija, nieta)) {
            lenient().when(categoriaRepository.findWithParentById(c.getId())).thenReturn(Optional.of(c));
            lenient().when(categoriaRepository.findById(c.getId())).thenReturn(Optional.of(c));
        }
        lenient().when(categoriaRepository.findBySlugIgnoreCase(anyString())).thenReturn(Optional.empty());
    }

    private CategoriaRequestDTO request(Categoria parent, String slug) {
        return new CategoriaRequestDTO(parent, "  Nueva  ", slug, true);
    }

    @Test
    void create_normalizaNombreYSlug() {
        Categoria mapeada = Categoria.builder().nombre("  Nueva  ").slug("  Mi-Slug ").build();
        when(categoriaMapper.toEntity(any())).thenReturn(mapeada);

        service.create(request(null, "  Mi-Slug "));

        assertEquals("Nueva", mapeada.getNombre());
        assertEquals("mi-slug", mapeada.getSlug());
        verify(categoriaRepository).findBySlugIgnoreCase("Mi-Slug");
    }

    @Test
    void create_slugDuplicado_lanzaValidation() {
        when(categoriaRepository.findBySlugIgnoreCase("plasticos")).thenReturn(Optional.of(raiz));

        assertThrows(ValidationException.class, () -> service.create(request(null, "plasticos")));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void update_conservaSuSlugYPadreValido_guarda() {
        when(categoriaRepository.findBySlugIgnoreCase("alveolar")).thenReturn(Optional.of(nieta));

        service.update(nieta.getId(), new CategoriaRequestDTO(hija, "Alveolar", "alveolar", true));

        verify(categoriaMapper).updateEntityFromDto(any(), eq(nieta));
        verify(categoriaRepository).save(nieta);
    }

    @Test
    void update_propioPadre_lanzaValidation() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(hija.getId(), new CategoriaRequestDTO(hija, "x", "x", true)));
        assertTrue(ex.getMessage().contains("su propio padre"));
    }

    @Test
    void update_padreEsSuDescendiente_lanzaValidationPorCiclo() {
        // raiz → hija → nieta; poner a raiz debajo de nieta crearía un ciclo
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.update(raiz.getId(), new CategoriaRequestDTO(nieta, "x", "x", true)));
        assertTrue(ex.getMessage().contains("descendencia"));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void update_padreInexistente_lanzaNotFound() {
        Categoria fantasma = Categoria.builder().id(UUID.randomUUID()).build();
        when(categoriaRepository.findById(fantasma.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(hija.getId(), new CategoriaRequestDTO(fantasma, "x", "x", true)));
    }

    @Test
    void update_padreSinId_noValidaJerarquia() {
        Categoria mapeada = hija;

        service.update(mapeada.getId(), new CategoriaRequestDTO(new Categoria(), "x", "x", true));

        verify(categoriaRepository, never()).findById(any());
    }

    @Test
    void update_slugDeOtraCategoria_lanzaValidation() {
        when(categoriaRepository.findBySlugIgnoreCase("plasticos")).thenReturn(Optional.of(raiz));

        assertThrows(ValidationException.class, () -> service.update(hija.getId(), request(null, "plasticos")));
    }

    @Test
    void findChildren_padreInexistente_lanzaNotFound() {
        UUID x = UUID.randomUUID();
        when(categoriaRepository.findWithParentById(x)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findChildren(x));
        verify(categoriaRepository, never()).findByParent_IdOrderByNombreAsc(any());
    }

    @Test
    void findChildren_yRoots_delegan() {
        when(categoriaRepository.findByParent_IdOrderByNombreAsc(raiz.getId())).thenReturn(List.of(hija));
        when(categoriaRepository.findByParentIsNullOrderByNombreAsc()).thenReturn(List.of(raiz));

        assertEquals(1, service.findChildren(raiz.getId()).size());
        assertEquals(1, service.findRoots().size());
    }

    @Test
    void search_terminoEnBlancoSeEnviaNulo() {
        Pageable pg = PageRequest.of(0, 10);
        when(categoriaRepository.search(null, true, pg)).thenReturn(Page.empty());

        service.search(" ", true, pg);

        verify(categoriaRepository).search(null, true, pg);
    }

    @Test
    void delete_existente_borra() {
        service.delete(hija.getId());

        verify(categoriaRepository).delete(hija);
    }

    @Test
    void findById_inexistente_lanzaNotFound() {
        UUID x = UUID.randomUUID();
        when(categoriaRepository.findWithParentById(x)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(x));
    }

    @Test
    void getDashboardStatistics_cuentaActivasEInactivas() {
        when(categoriaRepository.count()).thenReturn(8L);
        when(categoriaRepository.countByActiva(true)).thenReturn(6L);
        when(categoriaRepository.countByActiva(false)).thenReturn(2L);

        Map<String, Object> d = service.getDashboardStatistics();

        assertEquals(8L, d.get("totalCategorias"));
        assertEquals(6L, d.get("categoriasActivas"));
        assertEquals(2L, d.get("categoriasInactivas"));
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void isSlugDisponible_recortaYDistingueLaPropia() {
        when(categoriaRepository.findBySlugIgnoreCase("plasticos")).thenReturn(Optional.of(raiz));

        assertTrue(service.isSlugDisponible(" libre ", null));
        assertTrue(service.isSlugDisponible(" plasticos ", raiz.getId()));
        assertFalse(service.isSlugDisponible("plasticos", hija.getId()));
        verify(categoriaRepository).findBySlugIgnoreCase("libre");
    }
}
