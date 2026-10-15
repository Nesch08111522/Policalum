package com.policalum.erp.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.policalum.erp.application.dto.request.CategoriaRequestDTO;
import com.policalum.erp.application.dto.response.CategoriaResponseDTO;

public interface CategoriaService {

    CategoriaResponseDTO create(CategoriaRequestDTO requestDTO);

    CategoriaResponseDTO update(UUID id, CategoriaRequestDTO requestDTO);

    CategoriaResponseDTO findById(UUID id);

    Page<CategoriaResponseDTO> search(String searchTerm, Boolean activa, Pageable pageable);

    List<CategoriaResponseDTO> findRoots();

    List<CategoriaResponseDTO> findChildren(UUID parentId);

    void delete(UUID id);

    boolean isSlugDisponible(String slug, UUID excludeId);

    Map<String, Object> getDashboardStatistics();
}
