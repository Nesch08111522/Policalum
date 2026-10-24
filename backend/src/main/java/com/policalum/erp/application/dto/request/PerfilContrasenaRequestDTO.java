package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerfilContrasenaRequestDTO(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String actual,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_]).*$",
                message = "La contraseña debe contener mayúscula, minúscula, número y un carácter especial")
        String nueva,

        @NotBlank(message = "Confirma la nueva contraseña")
        String confirmacion
) {}
