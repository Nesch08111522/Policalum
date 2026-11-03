package com.policalum.erp.application.dto.response;

import java.util.List;
import java.util.UUID;

/** Lo que el visitante puede leer de su propio hilo. La clave es el acceso. */
public record HiloPublicoDTO(
        UUID id,
        String clave,
        String nombre,
        int noLeidos,
        boolean equipoEscribiendo,
        List<LineaChatDTO> lineas
) {}
