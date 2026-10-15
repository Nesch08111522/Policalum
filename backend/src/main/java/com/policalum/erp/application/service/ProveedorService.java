package com.policalum.erp.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.policalum.erp.application.dto.request.ProveedorRequestDTO;
import com.policalum.erp.application.dto.response.ProveedorResponseDTO;

public interface ProveedorService {

    ProveedorResponseDTO create(ProveedorRequestDTO requestDTO);

    ProveedorResponseDTO update(UUID id, ProveedorRequestDTO requestDTO);

    ProveedorResponseDTO findById(UUID id);

    ProveedorResponseDTO findByDocumento(String tipoDocumento, String numeroDocumento);

    void delete(UUID id);

    Page<ProveedorResponseDTO> search(String searchTerm, Pageable pageable);

    Page<ProveedorResponseDTO> findAll(Pageable pageable);

    Page<ProveedorResponseDTO> findByActivo(Boolean activo, Pageable pageable);

    List<ProveedorResponseDTO> findWithIncompleteContactData();

    Map<String, Object> getDashboardStatistics();

    Map<String, Long> getStatisticsByDocumentType();

    boolean isDocumentoDisponible(String tipoDocumento, String numeroDocumento, UUID excludeId);
}