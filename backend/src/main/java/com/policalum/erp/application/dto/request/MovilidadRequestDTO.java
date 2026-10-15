package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.TipoMovilidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Crear/editar vehículo (Módulo 16). La placa se guarda en mayúsculas y es única en toda la
 * empresa (se compara sin guiones ni espacios). activo solo aplica al editar (null = no cambia).
 * El conductor se asigna con POST /{id}/asignar-conductor.
 */
public record MovilidadRequestDTO(
        @NotNull UUID sedeId,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9 -]{5,10}", message = "Placa inválida (5 a 10 letras, números o guiones)") String placa,
        @NotNull TipoMovilidad tipo,
        @Size(max = 50) String marca,
        @Size(max = 50) String modelo,
        Boolean activo) { }
