package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.domain.enums.TipoComprobante;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** Supplies POS-only data. Lines are optional to permit a last controlled edit before confirmation. */
public record ConvertirCotizacionRequestDTO(
        @NotNull UUID almacenId, @NotNull UUID turnoCajaId, @NotNull TipoComprobante tipoComprobante,
        CanalVenta canalVenta, List<@Valid DetalleCotizacionCommandDTO> lineas,
        @NotEmpty List<@Valid PagoVentaCommandDTO> pagos) { }
