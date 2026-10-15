package com.policalum.erp.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Vista contable (devengado): la utilidad neta, consolidada y por sede. */
public record FlujoDevengadoResponseDTO(
        UUID sedeId,
        LocalDate desde,
        LocalDate hasta,
        UtilidadNetaResponseDTO consolidado,
        List<UtilidadSedeDTO> porSede) { }
