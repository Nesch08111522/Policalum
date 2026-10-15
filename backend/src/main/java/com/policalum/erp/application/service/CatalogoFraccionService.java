package com.policalum.erp.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.policalum.erp.application.dto.request.CatalogoFraccionRequestDTO;
import com.policalum.erp.application.dto.response.CatalogoFraccionResponseDTO;

public interface CatalogoFraccionService {

    CatalogoFraccionResponseDTO create(CatalogoFraccionRequestDTO requestDTO);

    CatalogoFraccionResponseDTO update(UUID id, CatalogoFraccionRequestDTO requestDTO);

    CatalogoFraccionResponseDTO findById(UUID id);

    Page<CatalogoFraccionResponseDTO> search(UUID productoId, Boolean activo,
            String searchTerm, Pageable pageable);

    List<CatalogoFraccionResponseDTO> findAvailableByProducto(UUID productoId);

    void delete(UUID id);

    boolean isFactorDisponible(UUID productoId, java.math.BigDecimal factor, UUID excludeId);

    Map<String, Object> getDashboardStatistics();
}
