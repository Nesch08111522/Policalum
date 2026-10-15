package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.TipoMovimiento;
import com.policalum.erp.domain.enums.TipoReferencia;
import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.KardexMovimiento;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.domain.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record KardexMovimientoRequestDTO(
        Producto producto,
        Almacen almacen,
        UUID referenciaId,
        TipoReferencia tipoReferencia,
        TipoMovimiento tipoMovimiento,
        BigDecimal cantidadMovida,
        BigDecimal stockResultante,
        User user,
        String observacion
) {
}
