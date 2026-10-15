package com.policalum.erp.application.dto.request;

import java.util.UUID;

/** conductorId null = quitar el conductor del vehículo. */
public record AsignarConductorRequestDTO(UUID conductorId) { }
