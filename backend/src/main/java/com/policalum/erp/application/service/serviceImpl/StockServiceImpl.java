package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.StockRequestDTO;
import com.policalum.erp.application.dto.response.StockResponseDTO;
import com.policalum.erp.application.mapper.StockMapper;
import com.policalum.erp.application.service.StockService;
import com.policalum.erp.domain.model.Stock;
import org.springframework.data.domain.Pageable;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockMapper stockMapper;

    @Override
    @Transactional(readOnly = true)
    public StockResponseDTO findByAlmacenAndProducto(UUID almacenId, UUID productoId) {
        Stock stock = stockRepository.findByAlmacen_IdAndProducto_Id(almacenId, productoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontraron existencias del producto en el almacén especificado"));
        return stockMapper.toResponseDto(stock);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockResponseDTO> searchByAlmacen(UUID almacenId, String searchTerm, boolean soloBajoStock, Pageable pageable) {
        String normalizedSearch = (searchTerm == null || searchTerm.isBlank()) ? null : searchTerm.trim();
        return stockRepository.searchByAlmacen(almacenId, normalizedSearch, soloBajoStock, pageable)
                .map(stockMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponseDTO> findByProducto(UUID productoId) {
        return stockRepository.findByProducto_Id(productoId).stream()
                .map(stockMapper::toResponseDto)
                .toList();
    }

    @Override
    public StockResponseDTO saveOrUpdate(StockRequestDTO dto) {
        Stock stock = stockRepository.findByAlmacen_IdAndProducto_Id(
                dto.almacen().getId(),
                dto.producto().getId()
        ).orElseGet(() -> Stock.builder()
                .producto(dto.producto())
                .almacen(dto.almacen())
                .build());

        stock.setCantidad(dto.cantidad());
        if (dto.stockMinimo() != null) {
            stock.setStockMinimo(dto.stockMinimo());
        }
        stock.setUpdatedAt(LocalDateTime.now());

        Stock saved = stockRepository.save(stock);
        return stockMapper.toResponseDto(saved);
    }
}