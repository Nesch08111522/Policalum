package com.policalum.erp.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Sin sedeId: consolidado de la empresa + desglose por sede. Con sedeId: solo esa sede. */
public record FlujoCajaRealResponseDTO(
        UUID sedeId,
        LocalDate desde,
        LocalDate hasta,
        FlujoCajaResumenDTO consolidado,
        List<FlujoCajaSedeDTO> porSede,
        List<String> advertencias) { }
