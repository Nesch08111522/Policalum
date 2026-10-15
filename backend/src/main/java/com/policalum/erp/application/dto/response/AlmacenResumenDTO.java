package com.policalum.erp.application.dto.response;

import java.util.UUID;

public record AlmacenResumenDTO(UUID almacenId, String almacenNombre, UUID sedeId, String sedeNombre) { }
