package com.policalum.erp.application.dto.response;


public record QrPublicoDTO(
        String payload,              // El código QR en texto plano
        String empleadoNombre,       // Nombre del empleado o "Sistema"
        long segundosRestantes,      // Tiempo restante para que expire
        long expiraEnEpoch          // Timestamp de expiración
) {}