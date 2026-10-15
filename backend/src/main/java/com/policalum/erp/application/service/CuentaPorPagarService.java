package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.PagoCxPRequestDTO;
import com.policalum.erp.application.dto.response.CuentaPorPagarResponseDTO;
import com.policalum.erp.application.dto.response.ResumenCxPDashboardDTO;
import com.policalum.erp.domain.enums.EstadoCxP;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CuentaPorPagarService {

    Page<CuentaPorPagarResponseDTO> buscar(UUID sedeId, UUID proveedorId, EstadoCxP estado,
                                           Boolean soloVencidas, Pageable pageable);

    CuentaPorPagarResponseDTO obtener(UUID id);

    CuentaPorPagarResponseDTO registrarPago(UUID id, PagoCxPRequestDTO dto);

    CuentaPorPagarResponseDTO obtenerPorCompra(UUID compraId);

    List<CuentaPorPagarResponseDTO> vencidas(UUID sedeId);

    List<CuentaPorPagarResponseDTO> proximasAVencer(UUID sedeId, int dias);

    ResumenCxPDashboardDTO dashboard(UUID sedeId);
}