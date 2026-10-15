package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferenciaRequestDTO(

        @NotNull(message = "El ID del producto es requerido")
        UUID productoId,

        @NotNull(message = "El ID del almacén de origen es requerido")
        UUID almacenOrigenId,

        @NotNull(message = "El ID del almacén de destino es requerido")
        UUID almacenDestinoId,

        @NotNull(message = "La cantidad a transferir es requerida")
        @DecimalMin(value = "0.0001", message = "La cantidad a transferir debe ser mayor a cero")
        BigDecimal cantidad,

        @Size(max = 255, message = "El motivo no debe exceder 255 caracteres")
        String motivo,

        @Size(max = 100, message = "La referencia del documento no debe exceder 100 caracteres")
        String referenciaDocumento
) {}