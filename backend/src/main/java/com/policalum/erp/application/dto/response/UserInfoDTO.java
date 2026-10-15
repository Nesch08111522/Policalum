// src/main/java/com/policalum/erp/application/dto/response/UserInfoDTO.java
package com.policalum.erp.application.dto.response;

import java.util.UUID;

public record UserInfoDTO(
        UUID id,
        String nombre,
        String apellido,
        String email,
        String nombreCompleto
) {}