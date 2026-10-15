package com.policalum.erp.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.policalum.erp.domain.enums.ClienteTipoDocumentoEnum;

public record ClienteResponseDTO(
        UUID id,
        ClienteTipoDocumentoEnum tipoDocumento,
        String numeroDocumento,
        String nombre,
        String email,
        String celular,
        String direccion,
        LocalDateTime createdAt) {
}
