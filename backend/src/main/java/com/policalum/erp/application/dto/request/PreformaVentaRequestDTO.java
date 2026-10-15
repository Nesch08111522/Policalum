package com.policalum.erp.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Alta o edición de una preforma.
 *
 * @param minutosVigencia opcional: 5 a 4320 (72 h). Al crear, null = 120 (2 h).
 *                        Al editar, si se envía la vigencia se reinicia desde ahora.
 */
public record PreformaVentaRequestDTO(
        @NotNull(message = "La sede es obligatoria")
        UUID sedeId,

        UUID clienteId,

        @Min(value = 5, message = "La vigencia mínima es 5 minutos")
        @Max(value = 4320, message = "La vigencia máxima es 72 horas (4320 minutos)")
        Integer minutosVigencia,

        @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres")
        String observaciones,

        @NotEmpty(message = "Debe incluir al menos una línea")
        @Size(max = 100, message = "Máximo 100 líneas")
        List<@Valid DetallePreformaRequestDTO> lineas
) { }
