package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de la empresa (Módulo 14). PUT reemplaza todo: los campos opcionales enviados en null quedan vacíos.
 * El dígito verificador del RUC se valida en el servicio. nombreComercial vacío = razón social.
 */
public record EmpresaRequestDTO(
        @NotBlank @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos") String ruc,
        @NotBlank @Size(max = 200) String razonSocial,
        @Size(max = 150) String nombreComercial,
        @NotBlank @Size(max = 300) String direccionFiscal,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "El ubigeo debe tener 6 dígitos (código INEI)") String ubigeo,
        @Pattern(regexp = "[0-9+() -]{6,20}", message = "Teléfono inválido (6 a 20 dígitos, +, (), espacios o guiones)") String telefono,
        @Email @Size(max = 150) String email,
        @Size(max = 500) @Pattern(regexp = "https?://\\S+", message = "logoUrl debe ser una URL http(s)") String logoUrl) { }
