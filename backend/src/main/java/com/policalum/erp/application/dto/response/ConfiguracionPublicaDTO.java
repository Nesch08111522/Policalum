package com.policalum.erp.application.dto.response;

/** Datos de marca para el login y documentos públicos. Sin autenticación: nunca agregar RUC ni datos internos. */
public record ConfiguracionPublicaDTO(String nombreComercial, String logoUrl) { }
