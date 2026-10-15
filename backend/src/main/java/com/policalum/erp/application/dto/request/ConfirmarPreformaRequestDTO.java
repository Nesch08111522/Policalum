package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.domain.enums.TipoComprobante;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Datos de caja para convertir la preforma en venta. El turno se toma automáticamente:
 * el turno ABIERTO de quien confirma en la sede de la preforma.
 */
public record ConfirmarPreformaRequestDTO(
        @NotNull(message = "El almacén de salida es obligatorio")
        UUID almacenId,

        @NotNull(message = "El tipo de comprobante es obligatorio")
        TipoComprobante tipoComprobante,

        CanalVenta canalVenta,

        @NotEmpty(message = "Debe registrar al menos un pago")
        List<@Valid PagoVentaCommandDTO> pagos
) { }
