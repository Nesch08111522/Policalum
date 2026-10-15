package com.policalum.erp.application.dto.response;

import java.util.UUID;

public record FlujoCajaSedeDTO(UUID sedeId, String sedeNombre, FlujoCajaResumenDTO resumen) { }
