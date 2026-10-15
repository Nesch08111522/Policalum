package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.policalum.erp.application.dto.request.StockRequestDTO;
import com.policalum.erp.application.dto.response.StockResponseDTO;
import com.policalum.erp.application.service.StockService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Inventario")
@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/almacen/{almacenId}")
    public ResponseEntity<Page<StockResponseDTO>> getStockByAlmacen(
            @PathVariable UUID almacenId,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "false") boolean soloBajoStock,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(stockService.searchByAlmacen(almacenId, searchTerm, soloBajoStock, pageable));
    }

    @GetMapping("/almacen/{almacenId}/producto/{productoId}")
    public ResponseEntity<StockResponseDTO> getStockByAlmacenAndProducto(
            @PathVariable UUID almacenId,
            @PathVariable UUID productoId) {
        return ResponseEntity.ok(stockService.findByAlmacenAndProducto(almacenId, productoId));
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<StockResponseDTO>> getStockByProducto(@PathVariable UUID productoId) {
        return ResponseEntity.ok(stockService.findByProducto(productoId));
    }

    @PostMapping
    public ResponseEntity<StockResponseDTO> saveOrUpdate(@Valid @RequestBody StockRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.saveOrUpdate(dto));
    }
}