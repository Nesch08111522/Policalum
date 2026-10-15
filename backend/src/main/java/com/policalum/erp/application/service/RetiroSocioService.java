package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.AnularRetiroRequestDTO;
import com.policalum.erp.application.dto.request.RetiroSocioRequestDTO;
import com.policalum.erp.application.dto.response.RetiroSocioDashboardDTO;
import com.policalum.erp.application.dto.response.RetiroSocioListadoDTO;
import com.policalum.erp.application.dto.response.RetiroSocioResponseDTO;
import com.policalum.erp.domain.enums.EstadoRetiro;
import com.policalum.erp.domain.enums.OrigenFondos;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Retiros de socios (separación patrimonial dueño/empresa). Solo ROLE_ADMIN. */
public interface RetiroSocioService {

    /**
     * Registra un retiro del admin autenticado. Si el origen es CAJA_FISICA lo vincula a su
     * turno abierto en la sede, valida efectivo suficiente y lo descuenta del turno.
     *
     * @throws IllegalStateException si no hay turno abierto o el efectivo no alcanza (409)
     */
    RetiroSocioResponseDTO registrar(RetiroSocioRequestDTO request);

    /** Detalle de un retiro. */
    RetiroSocioResponseDTO obtener(UUID id);

    /** Listado paginado con filtros opcionales; fechas inclusivas por día. */
    Page<RetiroSocioListadoDTO> buscar(UUID adminId, UUID sedeId, EstadoRetiro estado, OrigenFondos origen,
                                       LocalDate desde, LocalDate hasta, Pageable pageable);

    /**
     * Anula un retiro. Devuelve el monto a la caja solo si el turno vinculado sigue ABIERTO.
     *
     * @throws IllegalStateException si el retiro ya está anulado (409)
     */
    RetiroSocioResponseDTO anular(UUID id, AnularRetiroRequestDTO request);

    /** Totales del periodo (por defecto, mes en curso). */
    RetiroSocioDashboardDTO dashboard(UUID sedeId, LocalDate desde, LocalDate hasta);
}
