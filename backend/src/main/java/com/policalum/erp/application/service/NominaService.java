package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.GenerarNominaRequestDTO;
import com.policalum.erp.application.dto.request.PagarNominaRequestDTO;
import com.policalum.erp.application.dto.response.BoletaPagoDTO;
import com.policalum.erp.application.dto.response.GeneracionNominaResponseDTO;
import com.policalum.erp.application.dto.response.NominaDashboardDTO;
import com.policalum.erp.application.dto.response.NominaListadoDTO;
import com.policalum.erp.application.dto.response.NominaResponseDTO;
import com.policalum.erp.domain.enums.EstadoNomina;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Nómina mensual por empleado. Flujo: PENDIENTE → APROBADA → PAGADA. */
public interface NominaService {

    /**
     * Genera una nómina PENDIENTE por cada empleado con legajo activo de la sede.
     * Omite a quien ya tiene nómina en el periodo. Incluye la comisión APROBADA del periodo.
     *
     * @throws IllegalStateException si hay comisiones CALCULADAS sin aprobar en la sede (409)
     * @throws IllegalArgumentException si el periodo es futuro (400)
     */
    GeneracionNominaResponseDTO generar(GenerarNominaRequestDTO request);

    /** Listado paginado con filtros opcionales. Solo ADMIN. */
    Page<NominaListadoDTO> buscar(String periodo, UUID sedeId, EstadoNomina estado, UUID userId, Pageable pageable);

    /** Detalle de una nómina. Solo ADMIN. */
    NominaResponseDTO obtener(UUID id);

    /** PENDIENTE → APROBADA. Solo ADMIN. */
    NominaResponseDTO aprobar(UUID id);

    /** APROBADA → PAGADA. Marca la comisión incluida como LIQUIDADA. No mueve caja. Solo ADMIN. */
    NominaResponseDTO pagar(UUID id, PagarNominaRequestDTO request);

    /** Boleta de pago. El ADMIN ve cualquiera; un empleado solo la suya. */
    BoletaPagoDTO boleta(UUID id);

    /** Elimina una nómina PENDIENTE (la comisión vuelve a quedar libre). Solo ADMIN. */
    void eliminar(UUID id);

    /** Totales del periodo por estado (por defecto, mes en curso). Solo ADMIN. */
    NominaDashboardDTO dashboard(String periodo, UUID sedeId);
}
