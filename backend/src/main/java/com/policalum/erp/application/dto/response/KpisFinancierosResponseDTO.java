package com.policalum.erp.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record KpisFinancierosResponseDTO(
        UUID sedeId,
        LocalDate desde,
        LocalDate hasta,
        KpisFinancierosDTO consolidado,
        List<KpisSedeDTO> porSede) { }
