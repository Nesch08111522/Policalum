package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/**
 * @param periodo "YYYY-MM"
 * @param sedeId  opcional: solo vendedores de esa sede (sus ventas cuentan en todas las sedes)
 */
public record CalcularComisionesRequestDTO(
        @NotBlank(message = "El periodo es obligatorio")
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Periodo inválido, formato YYYY-MM")
        String periodo,

        UUID sedeId
) { }
