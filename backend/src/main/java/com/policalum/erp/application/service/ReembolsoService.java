package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.ProcesarReembolsoRequestDTO;
import com.policalum.erp.application.dto.request.ReembolsoRequestDTO;
import com.policalum.erp.application.dto.response.ReembolsoResponseDTO;
import com.policalum.erp.domain.enums.EstadoReembolso;
import com.policalum.erp.domain.enums.MetodoReembolso;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Reembolsos de devoluciones aprobadas. Flujo: PENDIENTE → PROCESADO. */
public interface ReembolsoService {

    /**
     * Crea el reembolso (uno por devolución APROBADA). Monto por defecto = total de la
     * devolución; puede ser menor con motivo, nunca mayor.
     */
    ReembolsoResponseDTO crear(ReembolsoRequestDTO request);

    /** Detalle. */
    ReembolsoResponseDTO obtener(UUID id);

    /** Listado paginado; fechas inclusivas sobre la fecha de creación. */
    Page<ReembolsoResponseDTO> buscar(UUID sedeId, EstadoReembolso estado, MetodoReembolso metodo,
                                      LocalDate desde, LocalDate hasta, Pageable pageable);

    /**
     * Marca PROCESADO (solo ADMIN). Si es EFECTIVO, sale del turno de caja ABIERTO del admin
     * en la sede de la devolución y valida que alcance el efectivo.
     *
     * @throws IllegalStateException sin turno abierto o efectivo insuficiente (409)
     */
    ReembolsoResponseDTO procesar(UUID id, ProcesarReembolsoRequestDTO request);
}
