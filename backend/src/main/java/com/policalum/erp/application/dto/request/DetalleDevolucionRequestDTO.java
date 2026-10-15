package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.EstadoProducto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param cantidad en las mismas unidades de la línea de venta (ej. 2 medias planchas = 2)
 */
public record DetalleDevolucionRequestDTO(
        @NotNull(message = "La línea de venta es obligatoria")
        UUID detalleVentaId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
        @Digits(integer = 8, fraction = 4, message = "Cantidad inválida")
        BigDecimal cantidad,

        @NotNull(message = "El estado del producto es obligatorio (BUENO, DAÑADO o USADO)")
        EstadoProducto estadoProducto,

        @Size(max = 500, message = "La observación no puede superar 500 caracteres")
        String observacion
) { }
