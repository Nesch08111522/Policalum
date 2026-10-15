package com.policalum.erp.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/** Detalle de un vehículo. ordenesActivas = despachos PENDIENTE o EN_CAMINO asignados. */
public record MovilidadResponseDTO(
        UUID id,
        String placa,
        String tipo,
        String marca,
        String modelo,
        UUID sedeId,
        String sedeNombre,
        UUID conductorId,
        String conductorNombre,
        Boolean activo,
        long ordenesActivas,
        LocalDateTime createdAt) { }
