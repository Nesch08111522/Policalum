// src/main/java/com/policalum/erp/application/service/RrhhAsistenciaService.java
package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.AsistenciaQrDTO;
import com.policalum.erp.application.dto.request.RrhhAsistenciaRequestDTO;
import com.policalum.erp.application.dto.response.AsistenciaEstadoDTO;
import com.policalum.erp.application.dto.response.DashboardAsistenciaDTO;
import com.policalum.erp.application.dto.response.RrhhAsistenciaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RrhhAsistenciaService {

    // ============================================
    // ✅ MÉTODOS EXISTENTES
    // ============================================

    RrhhAsistenciaResponseDTO procesarMarcacionQr(AsistenciaQrDTO qrRequest);

    RrhhAsistenciaResponseDTO procesarMarcacionManual(RrhhAsistenciaRequestDTO manualRequest);

    Page<RrhhAsistenciaResponseDTO> listarTodas(Pageable pageable);

    Page<RrhhAsistenciaResponseDTO> buscarPorEmpleado(UUID empleadoId, Pageable pageable);

    RrhhAsistenciaResponseDTO buscarPorId(UUID id);

    RrhhAsistenciaResponseDTO actualizarMarcacionManual(UUID id, RrhhAsistenciaRequestDTO updateRequest);

    void eliminarMarcacion(UUID id);

    // ============================================
    // ✅ NUEVOS MÉTODOS
    // ============================================

    /**
     * Listar asistencias con filtros (empleado y rango de fechas)
     */
    Page<RrhhAsistenciaResponseDTO> listarAsistencias(
            UUID empleadoId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable);

    /**
     * Obtener solo las asistencias de un empleado específico (para MULTIFUNCIONAL)
     */
    Page<RrhhAsistenciaResponseDTO> misAsistencias(
            UUID empleadoId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Pageable pageable);

    /**
     * Obtener dashboard con resumen de asistencias
     */
    DashboardAsistenciaDTO obtenerDashboard(UUID empleadoId);

    /**
     * Obtener la última marcación de un empleado
     */
    RrhhAsistenciaResponseDTO obtenerUltimaMarcacion(UUID empleadoId);

    /**
     * Obtener el estado del día de un empleado (entrada/salida)
     */
    AsistenciaEstadoDTO obtenerEstadoDelDia(UUID empleadoId);
}