package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.TipoCargoCotizacion;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FormatoCotizacionDTO(
        BigDecimal utilidadPorcentaje,
        List<DistritoTarifaDTO> distritos,
        List<ConceptoCargoDTO> conceptos
) {
    public record DistritoTarifaDTO(
            UUID id,
            String distrito,
            BigDecimal precioBase,
            Integer unidadesIncluidas,
            BigDecimal precioUnidadExtra,
            Boolean activo
    ) {}

    public record ConceptoCargoDTO(
            UUID id,
            String nombre,
            TipoCargoCotizacion tipo,
            BigDecimal valor,
            Boolean activo
    ) {}
}
