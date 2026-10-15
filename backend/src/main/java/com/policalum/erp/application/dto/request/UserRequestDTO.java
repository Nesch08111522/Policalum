package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.domain.model.Sede;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(

        @NotNull(message = "El rol es requerido") Rol rol,

        @NotNull(message = "La Sede es requerida") Sede sede,

        @NotBlank(message = "El nombre de usuario es requerido") @Size(min = 4, max = 20, message = "El nombre de usuario debe tener entre 4 y 20 caracteres") @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "El nombre de usuario solo puede contener letras, números y guiones bajos") String nombreUsuario,

        @NotBlank(message = "El nombre es requerido") @Size(max = 50, message = "El nombre no puede exceder los 50 caracteres") String nombre,

        @NotBlank(message = "El apellido es requerido") @Size(max = 50, message = "El apellido no puede exceder los 50 caracteres") String apellido,

        @NotBlank(message = "El email es requerido") @Email(message = "El formato del email no es válido") String email,

        @NotBlank(message = "La contraseña es requerida") @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_]).*$", message = "La contraseña debe contener al menos una mayúscula, una minúscula, un número y un carácter especial") String passwordHash,

        @NotBlank(message = "El celular es requerido") @Pattern(regexp = "^9[0-9]{8}$", message = "El celular debe ser un número válido de 9 dígitos y empezar con 9") String celular,

        Boolean activo)
{



}