package com.policalum.erp.application.dto.response;

import java.util.UUID;

public record ProveedorResponseDTO(
        UUID id,
        String nombreRazon,
        String tipoDocumento,
        String numeroDocumento,
        String celular,
        String email,
        String direccion,
        String condicionesPago,
        Integer diasCredito,
        String banco,
        String cuentaBancaria,
        Boolean activo
) { }