package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.ClienteRequestDTO;
import com.policalum.erp.application.dto.response.ClienteResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ClienteService {

    // ============ CRUD OPERATIONS ============
    ClienteResponseDTO create(ClienteRequestDTO requestDTO);
    ClienteResponseDTO update(UUID id, ClienteRequestDTO requestDTO);
    ClienteResponseDTO findById(UUID id);
    ClienteResponseDTO findByNumeroDocumento(String numeroDocumento);
    /** Null si se borró porque no tenía historial. El DTO inactivo si se conservó. */
    ClienteResponseDTO delete(UUID id);

    // ============ SEARCH & FILTER ============
    Page<ClienteResponseDTO> search(String searchTerm, Pageable pageable);
    Page<ClienteResponseDTO> findAll(Pageable pageable);
    Page<ClienteResponseDTO> findByTipoDocumento(String tipoDocumento, Pageable pageable);

    // ============ BUSINESS CONSULTATIONS ============
    List<ClienteResponseDTO> findTop10Recent();
    List<ClienteResponseDTO> findClientesWithIncompleteData();
    List<ClienteResponseDTO> findClientesWithPurchasesSince(LocalDateTime fechaDesde);
    List<ClienteResponseDTO> findClientesWithoutPurchasesSince(LocalDateTime fechaDesde);

    // ============ STATISTICS & REPORTS ============
    long countTotalClientes();
    long countClientesByDateRange(LocalDateTime start, LocalDateTime end);
    long countClientesRegistradosHoy();
    long countClientesByTipoDocumento(String tipoDocumento);
    Map<String, Long> getEstadisticasPorTipoDocumento();
    Map<String, Object> getDashboardStatistics();

    // ============ VALIDATIONS ============
    boolean existsByNumeroDocumento(String numeroDocumento);
    boolean existsByEmail(String email);
    boolean validateDocumentoUniqueness(String numeroDocumento, UUID excludeId);
}