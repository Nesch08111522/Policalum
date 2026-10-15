package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @param sedeId          opcional: por defecto la sede de la venta
 * @param fechaProgramada opcional: por defecto hoy; no puede ser pasada
 */
public record CrearDespachoRequestDTO(
        @NotNull(message = "La venta es obligatoria")
        UUID ventaId,

        UUID sedeId,

        @NotBlank(message = "La dirección de entrega es obligatoria")
        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccionEntrega,

        @Size(max = 255, message = "La referencia no puede superar 255 caracteres")
        String referenciaEntrega,

        @Size(max = 150, message = "El nombre de contacto no puede superar 150 caracteres")
        String contactoNombre,

        @Pattern(regexp = "^\\+?[0-9 ]{6,20}$", message = "Teléfono de contacto inválido")
        String contactoTelefono,

        @FutureOrPresent(message = "La fecha programada no puede ser pasada")
        LocalDate fechaProgramada,

        @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres")
        String observaciones
) { }
