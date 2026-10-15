package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.StockRequestDTO;
import com.policalum.erp.application.dto.response.StockResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface StockService {
    StockResponseDTO findByAlmacenAndProducto(UUID almacenId, UUID productoId);


    List<StockResponseDTO> findByProducto(UUID productoId);
    Page<StockResponseDTO> searchByAlmacen(UUID almacenId, String searchTerm, boolean soloBajoStock, Pageable pageable);
    StockResponseDTO saveOrUpdate(StockRequestDTO dto);

}
