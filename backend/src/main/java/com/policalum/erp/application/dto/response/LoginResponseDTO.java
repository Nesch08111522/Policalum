package com.policalum.erp.application.dto.response;

import java.util.UUID;

public record LoginResponseDTO(
        String token,
        UUID usuarioId,
        String nombre,
        String email,
        String rol,
        boolean totpConfigurado
) {}