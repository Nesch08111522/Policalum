package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.ServicioRequestDTO;
import com.policalum.erp.application.dto.response.ServicioResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface ServicioService {

    ServicioResponseDTO create(ServicioRequestDTO requestDTO);

    ServicioResponseDTO update(UUID id, ServicioRequestDTO requestDTO);

    ServicioResponseDTO findById(UUID id);

    Page<ServicioResponseDTO> search(String searchTerm, Boolean activo, BigDecimal precioMin, BigDecimal precioMax, Pageable pageable);

    void delete(UUID id);

    boolean isNombreDisponible(String nombre, UUID excludeId);

    Map<String, Object> getDashboardStatistics();
}
