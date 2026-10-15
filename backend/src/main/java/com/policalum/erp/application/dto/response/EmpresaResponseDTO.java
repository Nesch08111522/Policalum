package com.policalum.erp.application.dto.response;

import java.time.LocalDateTime;

/** Datos de la empresa. configurada = false mientras no se registre el RUC. */
public record EmpresaResponseDTO(
        String ruc,
        String razonSocial,
        String nombreComercial,
        String direccionFiscal,
        String ubigeo,
        String telefono,
        String email,
        String logoUrl,
        boolean configurada,
        String actualizadoPor,
        LocalDateTime updatedAt) { }
