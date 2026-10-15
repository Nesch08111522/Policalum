package com.policalum.erp.application.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.domain.enums.EstadoSunat;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.domain.enums.TipoComprobante;
import com.policalum.erp.domain.model.Cliente;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.TurnoCaja;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.domain.model.Venta;

import jakarta.validation.constraints.NotNull;

public record VentaRequestDTO(
        @NotNull(message = "La sede es requerida")
        Sede sede,

        @NotNull(message = "El usuario es requerido")
        User user,

        Cliente cliente,
        UUID cotizacionId,
        UUID preformaId,
        TurnoCaja turnoCaja,

        @NotNull(message = "El tipo de comprobante es requerido")
        TipoComprobante tipoComprobante,

        String serieCorrelativo,

        @NotNull(message = "El estado de la venta es requerido")
        EstadoVenta estado,

        @NotNull(message = "El subtotal neto es requerido")
        BigDecimal subtotalNeto,

        @NotNull(message = "El total de IGV es requerido")
        BigDecimal totalIgv,

        @NotNull(message = "El total es requerido")
        BigDecimal total,

        CanalVenta canalVenta,
        EstadoSunat estadoSunat,
        String hashCpe,
        String mensajeSunat,
        String referenciaPedidoOnline
) {
}
