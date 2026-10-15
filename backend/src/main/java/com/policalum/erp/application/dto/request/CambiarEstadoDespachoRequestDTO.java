package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.EstadoDespacho;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Transiciones permitidas por este endpoint:
 * PENDIENTE → EN_CAMINO | FALLIDO, EN_CAMINO → FALLIDO, FALLIDO → PENDIENTE (reprogramar).
 * ENTREGADO NO se marca aquí: se usa POST /{id}/evidencia (foto + quién recibe).
 *
 * @param motivo          obligatorio para FALLIDO
 * @param fechaProgramada opcional al reprogramar (FALLIDO → PENDIENTE)
 */
public record CambiarEstadoDespachoRequestDTO(
        @NotNull(message = "El estado es obligatorio")
        EstadoDespacho estado,

        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo,

        LocalDate fechaProgramada
) { }
