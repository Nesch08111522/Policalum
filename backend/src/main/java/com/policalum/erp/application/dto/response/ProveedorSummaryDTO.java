package com.policalum.erp.application.dto.response;
import java.util.UUID;

public record ProveedorSummaryDTO(
        UUID id,
        String razonSocial
) {}