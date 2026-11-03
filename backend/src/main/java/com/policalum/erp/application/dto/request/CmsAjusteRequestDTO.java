package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CmsAjusteRequestDTO(
        @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Usa un color hexadecimal de 6 dígitos") String primaryColor,
        @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Usa un color hexadecimal de 6 dígitos") String secondaryColor,
        @Min(14) @Max(22) int baseFontSize,
        @NotBlank @Pattern(regexp = "aether|claro") String themeMode
) {}
