package com.policalum.erp.application.dto.response;

import java.util.UUID;

public record UtilidadSedeDTO(UUID sedeId, String sedeNombre, UtilidadNetaResponseDTO utilidad) { }
