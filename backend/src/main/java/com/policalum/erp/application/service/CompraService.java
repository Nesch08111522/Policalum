package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.AnularCompraRequestDTO;
import com.policalum.erp.application.dto.request.CompraRequestDTO;
import com.policalum.erp.application.dto.request.RecepcionCompraRequestDTO;
import com.policalum.erp.application.dto.response.CompraListadoDTO;
import com.policalum.erp.application.dto.response.CompraResponseDTO;
import com.policalum.erp.domain.enums.EstadoCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface CompraService {

    CompraResponseDTO crear(CompraRequestDTO dto);

    CompraResponseDTO actualizar(UUID id, CompraRequestDTO dto);

    CompraResponseDTO obtener(UUID id);

    Page<CompraListadoDTO> buscar(UUID proveedorId, UUID sedeId, EstadoCompra estado,
                                  LocalDate desde, LocalDate hasta, Pageable pageable);

    CompraResponseDTO recibir(UUID id, RecepcionCompraRequestDTO dto);

    CompraResponseDTO anular(UUID id, AnularCompraRequestDTO dto);

    void eliminar(UUID id);

    Map<String, Object> dashboard();
}