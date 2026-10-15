package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.EstadoCxP;
import com.policalum.erp.domain.enums.TipoReferenciaCxP;
import com.policalum.erp.domain.model.FinanzasCuentaPorPagar;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FinanzasCuentaPorPagarRequestDTO(
        Sede sede,
        UUID referenciaId,
        TipoReferenciaCxP tipoReferencia,
        String descripcion,
        BigDecimal montoTotal,
        BigDecimal montoPagado,
        EstadoCxP estado,
        LocalDate fechaVencimiento,
        LocalDateTime fechaPago,
        String metodoPago,
        User pagadoPor
) {
}
