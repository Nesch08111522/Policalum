package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Size;

/** Cuerpo opcional al procesar: referencia de la operación (Yape, transferencia...). */
public record ProcesarReembolsoRequestDTO(
        @Size(max = 100, message = "La referencia no puede superar 100 caracteres")
        String referencia,

        @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres")
        String observaciones
) { }
