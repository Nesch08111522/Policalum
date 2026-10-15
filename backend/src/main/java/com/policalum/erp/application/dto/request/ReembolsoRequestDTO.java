package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.MetodoReembolso;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param monto        opcional: null = monto total de la devolución (el tope). Nunca puede superarlo.
 * @param motivoAjuste obligatorio si el monto es menor al tope
 */
public record ReembolsoRequestDTO(
        @NotNull(message = "La devolución es obligatoria")
        UUID devolucionId,

        @NotNull(message = "El método de reembolso es obligatorio")
        MetodoReembolso metodoReembolso,

        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "Monto inválido")
        BigDecimal monto,

        @Size(max = 500, message = "El motivo del ajuste no puede superar 500 caracteres")
        String motivoAjuste,

        @Size(max = 100, message = "La referencia no puede superar 100 caracteres")
        String referencia,

        @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres")
        String observaciones
) { }
