package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Size;

/** Cuerpo opcional al cancelar. */
public record CancelarPreformaRequestDTO(
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) { }
