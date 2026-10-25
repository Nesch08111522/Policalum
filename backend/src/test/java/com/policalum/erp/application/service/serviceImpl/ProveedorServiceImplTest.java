package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.ProveedorRequestDTO;
import com.policalum.erp.application.mapper.ProveedorMapper;
import com.policalum.erp.domain.model.Proveedor;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.ProveedorRepository;
import jakarta.validation.ValidationException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

/** Maestro de proveedores: unicidad por tipo + número de documento. */
@ExtendWith(MockitoExtension.class)
class ProveedorServiceImplTest {

    @Mock ProveedorRepository proveedorRepository;
    @Mock ProveedorMapper proveedorMapper;
    @InjectMocks ProveedorServiceImpl service;

    UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(proveedorRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(proveedorMapper.toEntity(any())).thenAnswer(i -> Proveedor.builder().id(UUID.randomUUID()).build());
    }

    private ProveedorRequestDTO request(String doc) {
        return new ProveedorRequestDTO("Aluminios SAC", "RUC", doc, "987654321", "ventas@alu.pe", "Av. 1",
                "CREDITO_30", 30, "BCP", "191-123", true);
    }

    private Proveedor existente() {
        Proveedor p = Proveedor.builder().id(id).tipoDocumento("RUC").numeroDocumento("20123456789").build();
        lenient().when(proveedorRepository.findById(id)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void create_documentoLibre_guarda() {
        when(proveedorRepository.findByTipoDocumentoAndNumeroDocumento("RUC", "20123456789")).thenReturn(Optional.empty());

        service.create(request("20123456789"));

        verify(proveedorRepository).save(any());
    }

    @Test
    void create_documentoUsado_lanzaValidationSinGuardar() {
        when(proveedorRepository.findByTipoDocumentoAndNumeroDocumento("RUC", "20123456789")).thenReturn(Optional.of(Proveedor.builder().id(UUID.randomUUID()).build()));

        assertThrows(ValidationException.class, () -> service.create(request("20123456789")));
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void update_conservaSuPropioDocumento_permitido() {
        Proveedor p = existente();
        when(proveedorRepository.findByTipoDocumentoAndNumeroDocumento("RUC", "20123456789")).thenReturn(Optional.of(p));

        service.update(id, request("20123456789"));

        verify(proveedorMapper).updateEntityFromDto(any(), eq(p));
        verify(proveedorRepository).save(p);
    }

    @Test
    void update_documentoDeOtroProveedor_lanzaValidation() {
        existente();
        Proveedor otro = Proveedor.builder().id(UUID.randomUUID()).build();
        when(proveedorRepository.findByTipoDocumentoAndNumeroDocumento("RUC", "20999999999")).thenReturn(Optional.of(otro));

        assertThrows(ValidationException.class, () -> service.update(id, request("20999999999")));
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void update_inexistente_lanzaNotFound() {
        when(proveedorRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(id, request("1")));
    }

    @Test
    void isDocumentoDisponible_casos() {
        Proveedor p = existente();
        when(proveedorRepository.findByTipoDocumentoAndNumeroDocumento("RUC", "20123456789")).thenReturn(Optional.of(p));
        when(proveedorRepository.findByTipoDocumentoAndNumeroDocumento("RUC", "libre")).thenReturn(Optional.empty());

        assertTrue(service.isDocumentoDisponible("RUC", "libre", null));
        assertTrue(service.isDocumentoDisponible("RUC", "20123456789", id), "es el mismo proveedor");
        assertFalse(service.isDocumentoDisponible("RUC", "20123456789", null));
        assertFalse(service.isDocumentoDisponible("RUC", "20123456789", UUID.randomUUID()));
    }

    @Test
    void findByDocumento_inexistente_lanzaNotFound() {
        when(proveedorRepository.findByTipoDocumentoAndNumeroDocumento("RUC", "1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByDocumento("RUC", "1"));
    }

    @Test
    void findById_inexistente_lanzaNotFound() {
        when(proveedorRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    @Test
    void delete_existente_borra() {
        Proveedor p = existente();

        service.delete(id);

        verify(proveedorRepository).delete(p);
    }

    @Test
    void delete_conHistorial_desactivaYNoBorra() {
        Proveedor p = existente();
        p.setActivo(true);
        when(proveedorRepository.tieneHistorial(id)).thenReturn(true);

        service.delete(id);

        assertFalse(p.getActivo());
        verify(proveedorRepository).save(p);
        verify(proveedorRepository, never()).delete(any());
    }

    @Test
    void delete_inactivoConHistorial_rechaza() {
        Proveedor p = existente();
        p.setActivo(false);
        when(proveedorRepository.tieneHistorial(id)).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.delete(id));
        verify(proveedorRepository, never()).save(any());
        verify(proveedorRepository, never()).delete(any());
    }

    @Test
    void search_terminoVacioUsaFindAllYTerminoSeRecorta() {
        Pageable pg = PageRequest.of(0, 10);
        when(proveedorRepository.findAll(pg)).thenReturn(Page.empty());
        when(proveedorRepository.searchByNombreOrDocumento("alu", pg)).thenReturn(Page.empty());

        service.search(" ", pg);
        service.search(" alu ", pg);

        verify(proveedorRepository).findAll(pg);
        verify(proveedorRepository).searchByNombreOrDocumento("alu", pg);
    }

    @Test
    void findByActivo_yContactoIncompleto_delegan() {
        Pageable pg = PageRequest.of(0, 10);
        when(proveedorRepository.findByActivo(true, pg)).thenReturn(new PageImpl<>(List.of(new Proveedor())));
        when(proveedorRepository.findWithIncompleteContactData()).thenReturn(List.of(new Proveedor(), new Proveedor()));

        assertEquals(1, service.findByActivo(true, pg).getTotalElements());
        assertEquals(2, service.findWithIncompleteContactData().size());
    }

    @Test
    void getDashboardStatistics_cuentaActivosInactivosYTipos() {
        when(proveedorRepository.count()).thenReturn(10L);
        when(proveedorRepository.countByActivo(true)).thenReturn(8L);
        when(proveedorRepository.countByActivo(false)).thenReturn(2L);
        when(proveedorRepository.findWithIncompleteContactData()).thenReturn(List.of(new Proveedor()));
        when(proveedorRepository.countGroupByTipoDocumento()).thenReturn(List.of(new Object[] {"RUC", 9L}, new Object[] {null, 1}));

        Map<String, Object> d = service.getDashboardStatistics();

        assertEquals(10L, d.get("totalProveedores"));
        assertEquals(8L, d.get("proveedoresActivos"));
        assertEquals(2L, d.get("proveedoresInactivos"));
        assertEquals(1, d.get("contactosIncompletos"));
        assertEquals(Map.of("RUC", 9L, "SIN_TIPO", 1L), d.get("distribucionPorTipoDocumento"));
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void findAll_paginaYMapea() {
        Pageable pg = PageRequest.of(0, 10);
        when(proveedorRepository.findAll(pg)).thenReturn(new PageImpl<>(List.of(new Proveedor())));

        assertEquals(1, service.findAll(pg).getTotalElements());
    }

    @Test
    void getStatisticsByDocumentType_tipoNuloComoSinTipo() {
        when(proveedorRepository.countGroupByTipoDocumento()).thenReturn(List.of(new Object[] {"RUC", 4L}, new Object[] {"DNI", 1}, new Object[] {null, 2L}));

        assertEquals(Map.of("RUC", 4L, "DNI", 1L, "SIN_TIPO", 2L), service.getStatisticsByDocumentType());
    }
}
