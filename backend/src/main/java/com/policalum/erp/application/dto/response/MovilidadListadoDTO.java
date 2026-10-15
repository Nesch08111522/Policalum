package com.policalum.erp.application.dto.response;

import java.util.UUID;

public record MovilidadListadoDTO(
        UUID id,
        String placa,
        String tipo,
        String marca,
        String modelo,
        String sedeNombre,
        String conductorNombre,
        Boolean activo) { }
