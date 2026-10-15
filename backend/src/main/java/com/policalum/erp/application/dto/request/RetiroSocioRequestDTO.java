package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.OrigenFondos;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Registro de un retiro de socio.
 *
 * @param montoRetirado monto en soles (PEN), mayor a cero
 * @param origenFondos  CAJA_FISICA (sale del turno abierto del admin) o CUENTA_BANCARIA
 * @param sedeId        sede del retiro; si es null se usa la sede del admin autenticado
 * @param descripcion   detalle libre (opcional)
 */
public record RetiroSocioRequestDTO(
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "Monto inválido (máx. 10 enteros y 2 decimales)")
        BigDecimal montoRetirado,

        @NotNull(message = "El origen de fondos es obligatorio")
        OrigenFondos origenFondos,

        UUID sedeId,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion
) { }
