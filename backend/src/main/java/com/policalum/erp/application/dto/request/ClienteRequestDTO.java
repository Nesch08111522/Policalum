package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.ClienteTipoDocumentoEnum;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequestDTO(
        @NotNull(message = "El tipo de documento es requerido") ClienteTipoDocumentoEnum tipoDocumento,

        @NotBlank(message = "El número de documento es requerido") @Size(max = 20, message = "El número de documento no puede exceder 20 caracteres") String numeroDocumento,

        @NotBlank(message = "El nombre del cliente es requerido") @Size(max = 150, message = "El nombre no puede exceder 150 caracteres") String nombre,

        @Email(message = "El formato del email no es válido") @Size(max = 150, message = "El email no puede exceder 150 caracteres") String email,

        @Pattern(regexp = "^[0-9+\\-() ]*$", message = "El celular no es válido") @Size(max = 15, message = "El celular no puede exceder 15 caracteres") String celular,

        @Size(max = 255, message = "La dirección no puede exceder 255 caracteres") String direccion) {
}
