package com.policalum.erp.application.dto.response;

import java.util.UUID;

public record ActividadUsuarioDTO(UUID userId, String nombre, String email, long cantidad) { }
