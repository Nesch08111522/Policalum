package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

public record VentaResponseDTO(
        Sede sede,
        User user,
        Cliente cliente,
        UUID cotizacionId,
        UUID preformaId,
        TurnoCaja turnoCaja,
        TipoComprobante tipoComprobante,
        String serieCorrelativo,
        EstadoVenta estado,
        BigDecimal subtotalNeto,
        BigDecimal totalIgv,
        BigDecimal total,
        CanalVenta canalVenta,
        EstadoSunat estadoSunat,
        String hashCpe,
        String mensajeSunat,
        String referenciaPedidoOnline,
        LocalDateTime createdAt
) {
}
