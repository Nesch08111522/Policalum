package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.AsignarConductorRequestDTO;
import com.policalum.erp.application.dto.request.MovilidadRequestDTO;
import com.policalum.erp.application.dto.response.MovilidadDashboardDTO;
import com.policalum.erp.application.dto.response.MovilidadListadoDTO;
import com.policalum.erp.application.dto.response.MovilidadResponseDTO;
import com.policalum.erp.application.dto.response.OrdenDespachoResponseDTO;
import com.policalum.erp.application.dto.response.ReporteUsoMovilidadDTO;
import com.policalum.erp.domain.enums.EstadoDespacho;
import com.policalum.erp.domain.enums.TipoMovilidad;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Vehículos de reparto (Módulo 16). Escritura: solo ADMIN. Consulta: ADMIN y MULTIFUNCIONAL. */
public interface MovilidadService {

    /** Crea un vehículo activo, sin conductor. 409 si la placa ya existe en la empresa. */
    MovilidadResponseDTO crear(MovilidadRequestDTO request);

    /** Vehículos filtrados por sede, estado, tipo y texto (placa, marca, modelo o conductor). */
    Page<MovilidadListadoDTO> listar(UUID sedeId, Boolean activo, TipoMovilidad tipo, String texto, Pageable pageable);

    /** Detalle con la cantidad de despachos activos. 404 si no existe. */
    MovilidadResponseDTO obtener(UUID id);

    /** Edita datos; activo=false equivale a desactivar (mismas validaciones), activo=true reactiva. */
    MovilidadResponseDTO actualizar(UUID id, MovilidadRequestDTO request);

    /** Baja lógica. 409 si tiene despachos PENDIENTE o EN_CAMINO. */
    void desactivar(UUID id);

    /** Asigna (o quita, con conductorId null) el conductor. Debe ser MULTIFUNCIONAL activo. */
    MovilidadResponseDTO asignarConductor(UUID id, AsignarConductorRequestDTO request);

    /** Despachos del vehículo, más recientes primero. */
    Page<OrdenDespachoResponseDTO> historial(UUID id, EstadoDespacho estado, Pageable pageable);

    /** Entregados/fallidos por vehículo con fecha programada en el rango (sin fechas: mes en curso). */
    ReporteUsoMovilidadDTO reporteUso(UUID sedeId, LocalDate desde, LocalDate hasta);

    /** KPIs de la flota. */
    MovilidadDashboardDTO dashboard(UUID sedeId);
}
