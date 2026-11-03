package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Resultado oficial, calculado en el servidor, antes de guardar la cotización. */
public record VistaCotizacionDTO(
        BigDecimal total,
        CargosCotizacionDTO cargos,
        List<DetalleCotizacionResponseDTO> lineas
) {}
