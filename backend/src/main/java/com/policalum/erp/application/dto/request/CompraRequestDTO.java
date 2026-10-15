package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.Moneda;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CompraRequestDTO(
        @NotNull UUID proveedorId,
        @NotNull UUID sedeId,
        @Size(max = 50) String numeroFacturaProveedor,
        @NotNull LocalDate fechaEmision,
        Moneda moneda,                           // opcional, default PEN
        @Size(max = 1000) String observaciones,
        @NotEmpty List<@Valid DetalleCompraCommandDTO> detalles
) { }