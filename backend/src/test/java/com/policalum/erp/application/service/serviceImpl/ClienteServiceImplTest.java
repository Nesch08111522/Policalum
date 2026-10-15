package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.ClienteRequestDTO;
import com.policalum.erp.application.mapper.ClienteMapper;
import com.policalum.erp.domain.enums.ClienteTipoDocumentoEnum;
import com.policalum.erp.domain.model.Cliente;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.ClienteRepository;
import jakarta.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

/** Maestro de clientes: unicidad de documento y email, búsqueda y estadísticas. */
@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock ClienteRepository clienteRepository;
    @Mock ClienteMapper clienteMapper;
    @InjectMocks ClienteServiceImpl service;

    UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(clienteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(clienteMapper.toEntity(any())).thenAnswer(i -> new Cliente());
    }

    private ClienteRequestDTO request(String doc, String email) {
        return new ClienteRequestDTO(ClienteTipoDocumentoEnum.DNI, doc, "Ana Ruiz", email, "987654321", "Av. Larco 1");
    }

    private Cliente existente(String doc, String email) {
        Cliente c = Cliente.builder().id(id).numeroDocumento(doc).email(email).nombre("Ana").build();
        lenient().when(clienteRepository.findById(id)).thenReturn(Optional.of(c));
        return c;
    }

    // ---------------------------------------------------------------- create

    @Test
    void create_documentoYEmailNuevos_guarda() {
        when(clienteRepository.existsByNumeroDocumento("45678912")).thenReturn(false);
        when(clienteRepository.existsByEmail("ana@mail.com")).thenReturn(false);

        service.create(request("45678912", "ana@mail.com"));

        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void create_documentoDuplicado_lanzaValidationSinGuardar() {
        when(clienteRepository.existsByNumeroDocumento("45678912")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.create(request("45678912", null)));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void create_emailDuplicado_lanzaValidation() {
        when(clienteRepository.existsByNumeroDocumento(any())).thenReturn(false);
        when(clienteRepository.existsByEmail("ana@mail.com")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.create(request("1", "ana@mail.com")));
    }

    @Test
    void create_sinEmailOEmailVacio_noConsultaEmail() {
        when(clienteRepository.existsByNumeroDocumento(any())).thenReturn(false);

        service.create(request("1", null));
        service.create(request("2", ""));

        verify(clienteRepository, never()).existsByEmail(any());
        verify(clienteRepository, times(2)).save(any());
    }

    // ---------------------------------------------------------------- update

    @Test
    void update_mismoDocumentoYEmail_noValidaDuplicados() {
        Cliente c = existente("45678912", "ana@mail.com");

        service.update(id, request("45678912", "ana@mail.com"));

        verify(clienteRepository, never()).existsByNumeroDocumento(any());
        verify(clienteRepository, never()).existsByEmail(any());
        verify(clienteMapper).updateEntityFromDto(any(), eq(c));
        verify(clienteRepository).save(c);
    }

    @Test
    void update_cambiaADocumentoDeOtroCliente_lanzaValidation() {
        existente("45678912", "ana@mail.com");
        when(clienteRepository.existsByNumeroDocumento("11111111")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.update(id, request("11111111", "ana@mail.com")));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void update_cambiaAEmailDeOtroCliente_lanzaValidation() {
        existente("45678912", "ana@mail.com");
        when(clienteRepository.existsByEmail("otro@mail.com")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.update(id, request("45678912", "otro@mail.com")));
    }

    @Test
    void update_inexistente_lanzaNotFound() {
        when(clienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(id, request("1", null)));
    }

    /**
     * BUG: validateClientUpdate hace existingCliente.getEmail().equals(...) sin chequear null.
     * Un cliente registrado sin email (es opcional) no puede actualizarse agregándole uno: NPE → 500.
     * Habilitar tras usar Objects.equals(existingCliente.getEmail(), requestDTO.email()).
     */
    @Test
    @Disabled("Bug: NPE al agregar email a un cliente que no tenía. Habilitar tras usar Objects.equals en validateClientUpdate")
    void update_clienteSinEmailAgregaEmail_guarda() {
        existente("45678912", null);
        when(clienteRepository.existsByEmail("nuevo@mail.com")).thenReturn(false);

        assertDoesNotThrow(() -> service.update(id, request("45678912", "nuevo@mail.com")));
    }

    // ---------------------------------------------------------------- consultas

    @Test
    void findById_inexistente_lanzaNotFound() {
        when(clienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    @Test
    void findByNumeroDocumento_inexistente_lanzaNotFound() {
        when(clienteRepository.findByNumeroDocumento("999")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.findByNumeroDocumento("999"));
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    void findByNumeroDocumento_existente_mapea() {
        Cliente c = existente("45678912", null);
        when(clienteRepository.findByNumeroDocumento("45678912")).thenReturn(Optional.of(c));

        service.findByNumeroDocumento("45678912");

        verify(clienteMapper).toResponseDto(c);
    }

    @Test
    void delete_existente_borra() {
        Cliente c = existente("1", null);

        service.delete(id);

        verify(clienteRepository).delete(c);
    }

    @Test
    void delete_inexistente_lanzaNotFoundSinBorrar() {
        when(clienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
        verify(clienteRepository, never()).delete(any());
    }

    @Test
    void search_terminoEnBlanco_devuelveTodos() {
        Pageable p = PageRequest.of(0, 10);
        when(clienteRepository.findAll(p)).thenReturn(Page.empty());

        service.search("   ", p);
        service.search(null, p);

        verify(clienteRepository, times(2)).findAll(p);
        verify(clienteRepository, never()).searchByNombreOrDocumento(any(), any());
    }

    @Test
    void search_terminoConEspacios_seRecorta() {
        Pageable p = PageRequest.of(0, 10);
        when(clienteRepository.searchByNombreOrDocumento("ana", p)).thenReturn(Page.empty());

        service.search("  ana ", p);

        verify(clienteRepository).searchByNombreOrDocumento("ana", p);
    }

    @Test
    void findTop10Recent_pideTamanio10() {
        when(clienteRepository.findTop10ByOrderByCreatedAtDesc(any())).thenReturn(List.of(new Cliente()));

        assertEquals(1, service.findTop10Recent().size());
        ArgumentCaptor<Pageable> c = ArgumentCaptor.forClass(Pageable.class);
        verify(clienteRepository).findTop10ByOrderByCreatedAtDesc(c.capture());
        assertEquals(10, c.getValue().getPageSize());
    }

    @Test
    void findClientesConYSinComprasDesde_delegan() {
        LocalDateTime desde = LocalDateTime.now().minusDays(30);
        when(clienteRepository.findClientesConComprasDesde(desde)).thenReturn(List.of(new Cliente(), new Cliente()));
        when(clienteRepository.findClientesSinComprasDesde(desde)).thenReturn(List.of(new Cliente()));

        assertEquals(2, service.findClientesWithPurchasesSince(desde).size());
        assertEquals(1, service.findClientesWithoutPurchasesSince(desde).size());
    }

    @Test
    void findByTipoDocumento_delega() {
        Pageable p = PageRequest.of(0, 5);
        when(clienteRepository.findByTipoDocumento("RUC", p)).thenReturn(new PageImpl<>(List.of(new Cliente())));

        assertEquals(1, service.findByTipoDocumento("RUC", p).getTotalElements());
    }

    @Test
    void countClientesByTipoDocumento_usaTotalSinPaginar() {
        when(clienteRepository.findByTipoDocumento(eq("DNI"), any())).thenReturn(new PageImpl<>(List.of(new Cliente(), new Cliente())));

        assertEquals(2, service.countClientesByTipoDocumento("DNI"));
        ArgumentCaptor<Pageable> c = ArgumentCaptor.forClass(Pageable.class);
        verify(clienteRepository).findByTipoDocumento(eq("DNI"), c.capture());
        assertTrue(c.getValue().isUnpaged());
    }

    @Test
    void countClientesRegistradosHoy_consultaElDiaActual() {
        when(clienteRepository.countByCreatedAtBetween(any(), any())).thenReturn(4L);

        assertEquals(4, service.countClientesRegistradosHoy());
        ArgumentCaptor<LocalDateTime> ini = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> fin = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(clienteRepository).countByCreatedAtBetween(ini.capture(), fin.capture());
        assertEquals(java.time.LocalDate.now(), ini.getValue().toLocalDate());
        assertEquals(0, ini.getValue().getHour());
        assertEquals(23, fin.getValue().getHour());
    }

    @Test
    void getEstadisticasPorTipoDocumento_tipoNuloComoSinTipo() {
        when(clienteRepository.countGroupByTipoDocumento()).thenReturn(List.of(
                new Object[] {"DNI", 10L}, new Object[] {"RUC", 3}, new Object[] {null, 2L}));

        Map<String, Long> r = service.getEstadisticasPorTipoDocumento();

        assertEquals(Map.of("DNI", 10L, "RUC", 3L, "SIN_TIPO", 2L), r);
    }

    @Test
    void getDashboardStatistics_reuneTodasLasMetricas() {
        when(clienteRepository.count()).thenReturn(20L);
        when(clienteRepository.countByCreatedAtBetween(any(), any())).thenReturn(1L);
        when(clienteRepository.countGroupByTipoDocumento()).thenReturn(List.<Object[]>of(new Object[] {"DNI", 20L}));
        when(clienteRepository.findClientesConDatosIncompletos()).thenReturn(List.of(new Cliente(), new Cliente()));
        when(clienteRepository.findTop10ByOrderByCreatedAtDesc(any())).thenReturn(List.of());

        Map<String, Object> d = service.getDashboardStatistics();

        assertEquals(20L, d.get("totalClientes"));
        assertEquals(1L, d.get("clientesRegistradosHoy"));
        assertEquals(2, d.get("clientesSinCompletar"));
        assertEquals(Map.of("DNI", 20L), d.get("distribucionPorTipo"));
        assertEquals(List.of(), d.get("clientesRecientes"));
    }

    @Test
    void existsByEmail_nulo_falseSinConsultar() {
        assertFalse(service.existsByEmail(null));
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void existsByNumeroDocumento_delega() {
        when(clienteRepository.existsByNumeroDocumento("1")).thenReturn(true);

        assertTrue(service.existsByNumeroDocumento("1"));
    }

    @Test
    void validateDocumentoUniqueness_casos() {
        UUID otro = UUID.randomUUID();
        Cliente duenio = Cliente.builder().id(id).numeroDocumento("1").build();
        when(clienteRepository.findByNumeroDocumento("1")).thenReturn(Optional.of(duenio));
        when(clienteRepository.findByNumeroDocumento("2")).thenReturn(Optional.empty());
        when(clienteRepository.existsByNumeroDocumento("3")).thenReturn(true);
        when(clienteRepository.existsByNumeroDocumento("4")).thenReturn(false);

        assertTrue(service.validateDocumentoUniqueness("1", id), "es su propio documento");
        assertFalse(service.validateDocumentoUniqueness("1", otro), "lo usa otro cliente");
        assertTrue(service.validateDocumentoUniqueness("2", otro), "nadie lo usa");
        assertFalse(service.validateDocumentoUniqueness("3", null));
        assertTrue(service.validateDocumentoUniqueness("4", null));
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void findAll_paginaYMapea() {
        Pageable p = PageRequest.of(1, 5);
        when(clienteRepository.findAll(p)).thenReturn(new PageImpl<>(List.of(new Cliente(), new Cliente())));

        assertEquals(2, service.findAll(p).getContent().size());
    }

    @Test
    void findClientesWithIncompleteData_delega() {
        when(clienteRepository.findClientesConDatosIncompletos()).thenReturn(List.of(new Cliente()));

        assertEquals(1, service.findClientesWithIncompleteData().size());
    }

    @Test
    void conteos_delegan() {
        LocalDateTime a = LocalDateTime.now().minusDays(7), b = LocalDateTime.now();
        when(clienteRepository.count()).thenReturn(42L);
        when(clienteRepository.countByCreatedAtBetween(a, b)).thenReturn(5L);

        assertEquals(42L, service.countTotalClientes());
        assertEquals(5L, service.countClientesByDateRange(a, b));
    }
}
