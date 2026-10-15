package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * @param movilidadId opcional: si es null se usa la movilidad activa que tenga al repartidor
 *                    como conductor (si tiene exactamente una)
 */
public record AsignarRepartidorRequestDTO(
        @NotNull(message = "El repartidor es obligatorio")
        UUID responsableId,

        UUID movilidadId
) { }
