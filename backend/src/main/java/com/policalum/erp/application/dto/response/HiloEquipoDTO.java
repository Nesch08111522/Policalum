package com.policalum.erp.application.dto.response;

import java.util.List;
import java.util.UUID;

public record HiloEquipoDTO(
        UUID id,
        String nombre,
        String correo,
        String telefono,
        String empresa,
        int noLeidos,
        boolean clienteEscribiendo,
        List<LineaChatDTO> lineas,
        String estado,
        UUID clienteId
) {}
