package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PerfilResponseDTO(
        UUID id,
        String nombre,
        String apellido,
        String nombreUsuario,
        String email,
        String celular,
        String documento,
        String biografia,
        String foto,
        UUID sedeId,
        String sedeNombre,
        String rol,
        Boolean activo,
        Boolean totpEnabled,
        LocalDateTime ultimoLogin,
        String ultimoIp,
        boolean puedeEditarLaboral,
        UUID legajoId,
        BigDecimal sueldoBase,
        Integer diaCobro,
        String tipoContrato,
        LocalDate fechaIngreso,
        String sistemaPension,
        String token,
        String totpSecreto,
        String totpUrl
) {}
