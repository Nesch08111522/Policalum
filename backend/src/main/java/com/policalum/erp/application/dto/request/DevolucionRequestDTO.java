package com.policalum.erp.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Solicitud de devolución parcial o total de una venta COMPLETADA. */
public record DevolucionRequestDTO(
        @NotNull(message = "La venta es obligatoria")
        UUID ventaId,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 1000, message = "El motivo no puede superar 1000 caracteres")
        String motivo,

        @NotEmpty(message = "Debe indicar al menos una línea a devolver")
        @Size(max = 100, message = "Máximo 100 líneas por devolución")
        List<@Valid DetalleDevolucionRequestDTO> lineas
) { }
