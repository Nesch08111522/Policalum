package com.policalum.erp.application.dto.response;

/** Datos de contacto para el portal. Sin RUC ni parámetros internos. */
public record EmpresaPublicaDTO(
        String nombreComercial,
        String direccion,
        String telefono,
        String email,
        String whatsapp,
        String logoUrl
) {}
