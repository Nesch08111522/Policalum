package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ServicioPublicoDTO(UUID id, String nombre, String descripcion, BigDecimal precio) {}
