package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProveedorRequestDTO(
        @NotBlank(message = "La razón social es requerida")
        @Size(max = 255) String nombreRazon,

        @NotBlank(message = "El tipo de documento es requerido")
        @Size(max = 10) String tipoDocumento,

        @NotBlank(message = "El número de documento es requerido")
        @Size(max = 15) String numeroDocumento,

        @Pattern(regexp = "^[0-9+\\-() ]*$", message = "El celular no es válido")
        @Size(max = 15) String celular,

        @Email(message = "El formato del email no es válido")
        @Size(max = 150) String email,

        @Size(max = 255) String direccion,

        @Size(max = 20) String condicionesPago,

        @Min(0) @Max(365) Integer diasCredito,

        @Size(max = 100) String banco,

        @Size(max = 50) String cuentaBancaria,

        @NotNull(message = "El estado del proveedor es requerido")
        Boolean activo
) { }