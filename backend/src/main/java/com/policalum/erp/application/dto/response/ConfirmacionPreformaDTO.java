package com.policalum.erp.application.dto.response;

/** Resultado de confirmar: la preforma ya CONFIRMADA y la venta creada. */
public record ConfirmacionPreformaDTO(
        PreformaVentaResponseDTO preforma,
        VentaProcesadaResponseDTO venta
) { }
