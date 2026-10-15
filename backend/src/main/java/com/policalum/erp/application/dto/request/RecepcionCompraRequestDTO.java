package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Size;

public record RecepcionCompraRequestDTO(
        @Size(max = 50) String numeroFacturaProveedor,
        @Size(max = 1000) String observaciones
) { }