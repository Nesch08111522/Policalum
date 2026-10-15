package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.domain.enums.TipoComprobante;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** Command DTO: it deliberately contains identifiers, never JPA entities. */
public record RegistrarVentaRequestDTO(
        @NotNull UUID sedeId,
        @NotNull UUID almacenId,
        UUID clienteId,
        @NotNull UUID turnoCajaId,
        @NotNull TipoComprobante tipoComprobante,
        CanalVenta canalVenta,
        UUID cotizacionId,
        UUID preformaId,
        String referenciaPedidoOnline,
        @NotEmpty List<@Valid LineaVentaCommandDTO> lineas,
        @NotEmpty List<@Valid PagoVentaCommandDTO> pagos) { }
