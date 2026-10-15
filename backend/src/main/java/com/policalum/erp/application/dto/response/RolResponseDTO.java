package com.policalum.erp.application.dto.response;

import java.util.Map;
import java.util.UUID;

public record RolResponseDTO(UUID id,
                String nombre,
                Map<String, Object> permisosJson) {

}
