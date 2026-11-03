package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CmsAjusteRequestDTO(
        @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Usa un color hexadecimal de 6 dígitos") String primaryColor,
        @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Usa un color hexadecimal de 6 dígitos") String secondaryColor,
        @Min(14) @Max(22) int baseFontSize,
        @NotBlank @Pattern(regexp = "aether|claro") String themeMode,
        @Size(max = 180) String banner,
        @Size(max = 500) String resumen,
        @Size(max = 80) String horarioSemana,
        @Size(max = 80) String horarioSabado,
        @Size(max = 200) String facebook,
        @Size(max = 200) String instagram,
        @Size(max = 200) String linkedin,
        @Size(max = 200) String youtube,
        @Size(max = 150) String correoSoporte,
        @Size(max = 4000) String textoPrivacidad,
        @Size(max = 4000) String textoTerminos,
        @Size(max = 4000) String textoGarantia,
        @Size(max = 4000) String textoFaq
) {}
