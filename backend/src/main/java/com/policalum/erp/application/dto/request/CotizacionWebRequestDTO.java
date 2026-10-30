package com.policalum.erp.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Carrito del portal. Los precios se recalculan en el servidor. */
public record CotizacionWebRequestDTO(
        @NotBlank @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos") String ruc,
        @NotBlank @Size(max = 200) String razonSocial,
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank @Pattern(regexp = "[0-9+()\\- ]{6,20}", message = "Teléfono inválido") String telefono,
        @Size(max = 2000) String observaciones,
        @NotEmpty @Size(max = 40) List<@Valid DetalleCotizacionCommandDTO> lineas,
        /** Campo trampa para bots. El portal lo envía vacío. */
        @Size(max = 200) String website
) {}
