package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.AprobarDevolucionRequestDTO;
import com.policalum.erp.application.dto.request.DevolucionRequestDTO;
import com.policalum.erp.application.dto.request.RechazarDevolucionRequestDTO;
import com.policalum.erp.application.dto.response.DevolucionDashboardDTO;
import com.policalum.erp.application.dto.response.DevolucionListadoDTO;
import com.policalum.erp.application.dto.response.DevolucionResponseDTO;
import com.policalum.erp.domain.enums.EstadoDevolucion;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Devoluciones de ventas. Flujo: PENDIENTE → APROBADA (mueve Kardex) | RECHAZADA. */
public interface DevolucionService {

    /**
     * Registra una devolución PENDIENTE (parcial o total) con código DEV-AAAA-NNNNNN.
     * Valida que la venta esté COMPLETADA, que cada línea sea de esa venta, que no sea un
     * servicio y que no se devuelva más de lo vendido (contando devoluciones pendientes/aprobadas).
     */
    DevolucionResponseDTO crear(DevolucionRequestDTO request);

    /** Detalle con líneas y estado del reembolso. */
    DevolucionResponseDTO obtener(UUID id);

    /** Listado paginado; fechas inclusivas sobre la fecha de solicitud. */
    Page<DevolucionListadoDTO> buscar(UUID sedeId, EstadoDevolucion estado, UUID ventaId,
                                      LocalDate desde, LocalDate hasta, Pageable pageable);

    /** Todas las devoluciones de una venta, de la más reciente a la más antigua. */
    List<DevolucionListadoDTO> porVenta(UUID ventaId);

    /**
     * Aprueba (solo ADMIN): BUENO → Kardex ENTRADA (DEVOLUCION); DAÑADO/USADO → ENTRADA +
     * SALIDA MERMA (neto 0, queda trazado). Reingresa al almacén de la salida original.
     */
    DevolucionResponseDTO aprobar(UUID id, AprobarDevolucionRequestDTO request);

    /** Rechaza con motivo (solo ADMIN). No mueve stock. */
    DevolucionResponseDTO rechazar(UUID id, RechazarDevolucionRequestDTO request);

    /** KPIs del periodo (por defecto, mes en curso). Solo ADMIN. */
    DevolucionDashboardDTO dashboard(UUID sedeId, LocalDate desde, LocalDate hasta);
}
