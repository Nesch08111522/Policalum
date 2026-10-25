package com.policalum.erp.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.policalum.erp.application.dto.request.AlmacenRequestDTO;
import com.policalum.erp.application.dto.response.AlmacenResponseDTO;

public interface AlmacenService {

    AlmacenResponseDTO create(AlmacenRequestDTO requestDTO);

    AlmacenResponseDTO update(UUID id, AlmacenRequestDTO requestDTO);

    AlmacenResponseDTO findById(UUID id);

    Page<AlmacenResponseDTO> search(String searchTerm, UUID sedeId, Boolean activo, Pageable pageable);

    List<AlmacenResponseDTO> findBySede(UUID sedeId);

    /** Null si se borró porque no tenía historial. El DTO inactivo si se conservó. */
    AlmacenResponseDTO delete(UUID id);

    boolean isNombreDisponible(UUID sedeId, String nombre, UUID excludeId);

    Map<String, Object> getDashboardStatistics();
}
