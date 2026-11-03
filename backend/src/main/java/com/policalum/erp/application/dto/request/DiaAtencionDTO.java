package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DiaAtencionDTO(
        @NotBlank @Pattern(regexp = "LUNES|MARTES|MIERCOLES|JUEVES|VIERNES|SABADO|DOMINGO") String dia,
        @NotNull Boolean abierto,
        @Pattern(regexp = "|([01]\\d|2[0-3]):[0-5]\\d") String desde,
        @Pattern(regexp = "|([01]\\d|2[0-3]):[0-5]\\d") String hasta
) {}
