package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Nombre comercial y logo. No toca el RUC ni la dirección fiscal.
 * logoUrl null conserva el logo actual. Cadena vacía lo quita.
 */
public record MarcaRequestDTO(
        @NotBlank @Size(max = 150) String nombreComercial,
        @Size(max = 500)
        @Pattern(regexp = "^$|https?://\\S+|empresa/[A-Za-z0-9._/-]+", message = "El logo debe ser una URL http(s) o un archivo de la empresa")
        String logoUrl) {
}
