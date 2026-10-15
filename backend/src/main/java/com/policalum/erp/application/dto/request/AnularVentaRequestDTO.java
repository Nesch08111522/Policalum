package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/** An audit reason is mandatory because a completed sale cannot be deleted. */
public record AnularVentaRequestDTO(@NotBlank String motivo) { }
