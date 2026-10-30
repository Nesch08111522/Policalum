package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record FraccionPublicaDTO(UUID id, String nombre, BigDecimal factor, BigDecimal precio) {}
