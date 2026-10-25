package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.ConvertirCotizacionRequestDTO;
import com.policalum.erp.application.dto.request.CotizacionRequestDTO;
import com.policalum.erp.application.dto.request.RechazarCotizacionRequestDTO;
import com.policalum.erp.application.dto.response.CotizacionListadoDTO;
import com.policalum.erp.application.dto.response.CotizacionResponseDTO;
import com.policalum.erp.application.dto.response.VentaProcesadaResponseDTO;
import com.policalum.erp.domain.enums.EstadoCotizacion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CotizacionService {
    /** Creates a new quotation and calculates all commercial amounts in the server. */
    CotizacionResponseDTO create(CotizacionRequestDTO request);
    /** Updates an issued quotation only. */
    CotizacionResponseDTO update(UUID id, CotizacionRequestDTO request);
    /** Obtains the full quotation including lines. */
    CotizacionResponseDTO findById(UUID id);
    /** Searches quotations through their commercial filters. */
    Page<CotizacionListadoDTO> search(UUID clienteId, UUID sedeId, EstadoCotizacion estado, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
    /** Deletes an issued draft quotation only. */
    void delete(UUID id);
    /** Accepts an issued quotation. */
    CotizacionResponseDTO aceptar(UUID id);
    /** Rejects an issued quotation with an auditable reason. */
    CotizacionResponseDTO rechazar(UUID id, RechazarCotizacionRequestDTO request);
    /** Guarda la cotización como no comprada, sin tocar el stock. */
    CotizacionResponseDTO marcarNoComprada(UUID id);
    /** Converts an accepted quotation into the atomic POS sale workflow. */
    VentaProcesadaResponseDTO convertir(UUID id, ConvertirCotizacionRequestDTO request);
    /** Marks expired issued quotations and returns them. */
    List<CotizacionListadoDTO> vencidas();
    /** Returns summary counters used by commercial dashboards. */
    Map<String, Object> dashboard();
}
