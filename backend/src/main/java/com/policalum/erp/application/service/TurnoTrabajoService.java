package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.AsignarTurnoRequestDTO;
import com.policalum.erp.application.dto.request.TurnoTrabajoRequestDTO;
import com.policalum.erp.application.dto.response.AsignacionTurnoResponseDTO;
import com.policalum.erp.application.dto.response.TurnoDashboardDTO;
import com.policalum.erp.application.dto.response.TurnoDelDiaDTO;
import com.policalum.erp.application.dto.response.TurnoTrabajoResponseDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Turnos de trabajo por empleado (varios turnos rotativos permitidos, sin cruces).
 * Escritura: solo ADMIN. Lectura: ADMIN, o el empleado sobre sus propios turnos.
 */
public interface TurnoTrabajoService {

    /**
     * Crea un turno para un empleado activo.
     *
     * @throws IllegalStateException si se cruza con otro turno activo del empleado (409)
     */
    TurnoTrabajoResponseDTO crear(TurnoTrabajoRequestDTO request);

    /** Detalle. ADMIN o el dueño del turno. */
    TurnoTrabajoResponseDTO obtener(UUID id);

    /** Actualiza un turno (revalida cruces si queda activo). Solo ADMIN. */
    TurnoTrabajoResponseDTO actualizar(UUID id, TurnoTrabajoRequestDTO request);

    /** Desactiva el turno (baja lógica: las asistencias guardan su id). Solo ADMIN. */
    void eliminar(UUID id);

    /** Listado paginado con filtros opcionales. Solo ADMIN. */
    Page<TurnoTrabajoResponseDTO> buscar(UUID sedeId, UUID userId, Boolean activo, String nombreTurno, Pageable pageable);

    /** Mismo horario para varios empleados; todo o nada. Solo ADMIN. */
    AsignacionTurnoResponseDTO asignar(AsignarTurnoRequestDTO request);

    /** Turnos activos de un empleado. ADMIN o el propio empleado. */
    List<TurnoTrabajoResponseDTO> porEmpleado(UUID userId);

    /** KPIs de turnos (sedeId opcional). Solo ADMIN. */
    TurnoDashboardDTO dashboard(UUID sedeId);

    /**
     * Uso interno (asistencia, sin validar rol): turno activo del empleado para el día de
     * la marcación; si tiene varios ese día, el que empieza más cerca de la hora marcada.
     */
    Optional<TurnoDelDiaDTO> turnoDelDia(UUID userId, LocalDateTime marcacion);
}
