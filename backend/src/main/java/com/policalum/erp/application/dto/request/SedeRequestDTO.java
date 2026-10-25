package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SedeRequestDTO(
        @NotBlank(message = "El nombre de la sede es requerido") @Size(max = 100, message = "El nombre no puede exceder 100 caracteres") String nombre,

        @Size(max = 100, message = "El país no puede exceder 100 caracteres") String pais,

        @Size(max = 100, message = "La ciudad no puede exceder 100 caracteres") String ciudad,

        @Size(max = 255, message = "La dirección no puede exceder 255 caracteres") String direccion,

        @Pattern(regexp = "^[0-9+\\-() ]*$", message = "El celular de sede no es válido") @Size(max = 20, message = "El celular no puede exceder 20 caracteres") String celularSede,

        @Size(max = 10, message = "La moneda no puede exceder 10 caracteres") String moneda,

        Boolean activa,

        @DecimalMin(value = "-90.0", message = "La latitud no es válida") @DecimalMax(value = "90.0", message = "La latitud no es válida") Double latitud,

        @DecimalMin(value = "-180.0", message = "La longitud no es válida") @DecimalMax(value = "180.0", message = "La longitud no es válida") Double longitud,

        @Size(max = 500, message = "La imagen no puede exceder 500 caracteres") String imagen) {
}
