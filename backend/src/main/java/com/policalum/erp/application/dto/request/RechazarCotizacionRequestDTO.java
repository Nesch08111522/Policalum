package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record RechazarCotizacionRequestDTO(@NotBlank @Size(max = 1000) String motivo) { }
