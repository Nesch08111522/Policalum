package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.SistemaPension;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Alta o actualización del legajo de un empleado.
 *
 * @param empleadoId         id del User. En PUT debe coincidir con el de la URL.
 * @param tasaPension        ONP: opcional (13.00 por defecto). AFP: obligatoria (> 0). NINGUNO: null o 0.
 * @param porcentajeComision opcional, 0 a 100 (null = 0)
 * @param activo             opcional; null = true al crear y "sin cambio" al actualizar
 */
public record LegajoRequestDTO(
        @NotNull(message = "El empleado es obligatorio")
        UUID empleadoId,

        @NotBlank(message = "El tipo de contrato es obligatorio")
        @Pattern(regexp = "(?i)PLANILLA|RECIBO_HONORARIOS|CAS",
                message = "Tipo de contrato inválido: PLANILLA, RECIBO_HONORARIOS o CAS")
        String tipoContrato,

        @NotNull(message = "La fecha de ingreso es obligatoria")
        @PastOrPresent(message = "La fecha de ingreso no puede ser futura")
        LocalDate fechaIngreso,

        @NotNull(message = "El sueldo base es obligatorio")
        @DecimalMin(value = "0.01", message = "El sueldo base debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "Sueldo inválido")
        BigDecimal sueldoBase,

        @DecimalMin(value = "0.00", message = "La comisión no puede ser negativa")
        @DecimalMax(value = "100.00", message = "La comisión no puede superar 100%")
        @Digits(integer = 3, fraction = 2, message = "Porcentaje inválido")
        BigDecimal porcentajeComision,

        @NotNull(message = "El sistema de pensión es obligatorio (ONP, AFP o NINGUNO)")
        SistemaPension sistemaPension,

        @DecimalMin(value = "0.00", message = "La tasa de pensión no puede ser negativa")
        @DecimalMax(value = "100.00", message = "La tasa de pensión no puede superar 100%")
        @Digits(integer = 3, fraction = 2, message = "Tasa inválida")
        BigDecimal tasaPension,

        Boolean activo
) { }
