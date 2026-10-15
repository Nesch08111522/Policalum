package com.policalum.erp.application.dto.response;


import java.util.UUID;

public record TotpConfigDTO(
        UUID empleadoId,
        String secret,
        String qrCodeUrl,
        String mensaje
) {}