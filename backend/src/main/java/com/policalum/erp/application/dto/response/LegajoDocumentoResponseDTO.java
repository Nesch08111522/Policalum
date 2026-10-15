package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.TipoDocumentoLegajo;
import java.time.LocalDateTime;
import java.util.UUID;

/** Sin ruta en disco a propósito: se descarga por GET /legajos/{id}/documentos/{docId}. */
public record LegajoDocumentoResponseDTO(
        UUID id,
        UUID legajoId,
        TipoDocumentoLegajo tipo,
        String nombreOriginal,
        String contentType,
        Long tamanoBytes,
        String descripcion,
        String subidoPorNombre,
        LocalDateTime createdAt
) { }
