package com.policalum.erp.application.dto.response;

import java.util.UUID;

public record SedePublicaDTO(
        UUID id,
        String nombre,
        String ciudad,
        String direccion,
        String telefono) {}
