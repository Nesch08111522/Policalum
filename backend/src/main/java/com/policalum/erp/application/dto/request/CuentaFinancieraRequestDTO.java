package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.TipoCuentaFinanciera;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/**
 * sedeId null = cuenta corporativa. metodosPago puede ir vacío (p. ej. una cuenta de
 * ahorros sin movimientos del ERP): su saldo será el último registrado.
 */
public record CuentaFinancieraRequestDTO(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull TipoCuentaFinanciera tipo,
        @Size(max = 100) String entidad,
        @Pattern(regexp = "\\d{3,6}", message = "Indique solo los últimos 3 a 6 dígitos") String ultimosDigitos,
        UUID sedeId,
        Set<@NotNull MetodoPago> metodosPago) { }
