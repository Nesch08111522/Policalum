package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.RegistrarVentaRequestDTO;
import com.policalum.erp.application.dto.response.VentaProcesadaResponseDTO;
import com.policalum.erp.application.dto.request.AnularVentaRequestDTO;
import com.policalum.erp.application.dto.response.VentaDetalleResponseDTO;
import com.policalum.erp.application.dto.response.VentaListadoResponseDTO;
import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.domain.enums.EstadoVenta;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VentaService {
    VentaProcesadaResponseDTO registrar(RegistrarVentaRequestDTO request);
    VentaDetalleResponseDTO obtener(UUID id);
    Page<VentaListadoResponseDTO> buscar(UUID sedeId, UUID userId, UUID clienteId, EstadoVenta estado,
                                         CanalVenta canal, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    VentaProcesadaResponseDTO anular(UUID id, AnularVentaRequestDTO request);
}
