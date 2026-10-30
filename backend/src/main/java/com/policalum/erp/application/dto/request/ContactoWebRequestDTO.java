package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactoWebRequestDTO(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 150) String correo,
        @Size(max = 20) @Pattern(regexp = "|[0-9+()\\- ]{6,20}", message = "Teléfono inválido") String telefono,
        @Size(max = 150) String empresa,
        @NotBlank @Size(max = 120) String asunto,
        @NotBlank @Size(max = 2000) String mensaje,
        @Size(max = 200) String website
) {}
