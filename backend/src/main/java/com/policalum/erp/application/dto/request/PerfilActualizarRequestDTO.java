package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record PerfilActualizarRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
        String apellido,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        String email,

        @NotBlank(message = "El celular es obligatorio")
        @Size(max = 15, message = "El celular no puede exceder 15 caracteres")
        @Pattern(regexp = "[0-9+() -]{6,15}", message = "Teléfono inválido (6 a 15 dígitos, +, (), espacios o guiones)")
        String celular,

        @Pattern(regexp = "^$|^\\d{8}$|^\\d{11}$", message = "El documento debe ser un DNI de 8 dígitos o un RUC de 11")
        @Size(max = 15, message = "El documento no puede exceder 15 caracteres")
        String documento,

        @Size(max = 500, message = "La biografía no puede exceder 500 caracteres")
        String biografia,

        String foto,

        UUID sedeId,

        @DecimalMin(value = "0.01", message = "El sueldo base debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "Sueldo inválido")
        BigDecimal sueldoBase,

        @Min(value = 1, message = "El día de cobro va del 1 al 31")
        @Max(value = 31, message = "El día de cobro va del 1 al 31")
        Integer diaCobro
) {}
