package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Size;

/** Cuerpo opcional al aprobar. */
public record AprobarDevolucionRequestDTO(
        @Size(max = 1000, message = "La observación no puede superar 1000 caracteres")
        String observacion
) { }
