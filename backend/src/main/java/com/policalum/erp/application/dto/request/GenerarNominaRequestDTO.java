package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/**
 * @param periodo "YYYY-MM"
 * @param sedeId  se genera una nómina por cada empleado con legajo activo de esta sede
 */
public record GenerarNominaRequestDTO(
        @NotBlank(message = "El periodo es obligatorio")
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Periodo inválido, formato YYYY-MM")
        String periodo,

        @NotNull(message = "La sede es obligatoria")
        UUID sedeId
) { }
