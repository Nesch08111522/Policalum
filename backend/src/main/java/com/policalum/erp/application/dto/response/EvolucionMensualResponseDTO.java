package com.policalum.erp.application.dto.response;

import java.util.List;
import java.util.UUID;

/** Serie del mes más antiguo al más reciente (el último es el mes en curso). */
public record EvolucionMensualResponseDTO(UUID sedeId, int meses, List<MesEvolucionDTO> serie) { }
