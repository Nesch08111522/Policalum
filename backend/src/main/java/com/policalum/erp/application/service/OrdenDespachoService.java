package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.AsignarRepartidorRequestDTO;
import com.policalum.erp.application.dto.request.CambiarEstadoDespachoRequestDTO;
import com.policalum.erp.application.dto.request.CrearDespachoRequestDTO;
import com.policalum.erp.application.dto.request.EvidenciaEntregaRequestDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.DespachoDashboardDTO;
import com.policalum.erp.application.dto.response.OrdenDespachoResponseDTO;
import com.policalum.erp.domain.enums.EstadoDespacho;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Órdenes de despacho (delivery). Flujo:
 * PENDIENTE → EN_CAMINO → ENTREGADO (con evidencia) | FALLIDO; FALLIDO → PENDIENTE (reprogramar).
 */
public interface OrdenDespachoService {

    /**
     * Crea una orden PENDIENTE para una venta COMPLETADA y le asigna código DESP-AAAA-NNNNNN.
     * Una venta puede tener varias órdenes.
     */
    OrdenDespachoResponseDTO crear(CrearDespachoRequestDTO request);

    /** Detalle por id. */
    OrdenDespachoResponseDTO obtener(UUID id);

    /** Detalle por código de seguimiento (sin distinguir mayúsculas). */
    OrdenDespachoResponseDTO seguimiento(String codigo);

    /** Listado paginado; fechas inclusivas sobre la fecha programada. */
    Page<OrdenDespachoResponseDTO> buscar(UUID sedeId, EstadoDespacho estado, UUID responsableId, UUID ventaId,
                                          LocalDate desde, LocalDate hasta, Pageable pageable);

    /** Asigna o cambia repartidor/movilidad. Solo en PENDIENTE. */
    OrdenDespachoResponseDTO asignarRepartidor(UUID id, AsignarRepartidorRequestDTO request);

    /**
     * Cambia el estado (no permite ENTREGADO: usar registrarEntrega).
     * ADMIN o el repartidor asignado.
     *
     * @throws IllegalStateException si la transición no es válida (409)
     */
    OrdenDespachoResponseDTO cambiarEstado(UUID id, CambiarEstadoDespachoRequestDTO request);

    /**
     * Marca ENTREGADO con foto obligatoria y nombre de quien recibe. Solo desde EN_CAMINO.
     * ADMIN o el repartidor asignado.
     */
    OrdenDespachoResponseDTO registrarEntrega(UUID id, MultipartFile foto, EvidenciaEntregaRequestDTO datos);

    /** Descarga la foto de entrega. */
    ArchivoDescargaDTO descargarEvidencia(UUID id);

    /** Órdenes de un repartidor; por defecto solo las activas (PENDIENTE y EN_CAMINO). */
    List<OrdenDespachoResponseDTO> porRepartidor(UUID userId, boolean soloActivas);

    /** KPIs del día (por defecto hoy). */
    DespachoDashboardDTO dashboard(UUID sedeId, LocalDate fecha);
}
