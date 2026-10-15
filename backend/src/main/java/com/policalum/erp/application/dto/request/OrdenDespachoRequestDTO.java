package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.EstadoDespacho;
import com.policalum.erp.domain.model.Movilidad;
import com.policalum.erp.domain.model.OrdenDespacho;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.domain.model.Venta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OrdenDespachoRequestDTO(
        Venta venta,
        Sede sede,
        User responsable,
        Movilidad movilidad,
        String direccionEntrega,
        String referenciaEntrega,
        EstadoDespacho estado,
        LocalDate fechaProgramada,
        LocalDateTime fechaEntrega,
        String observaciones,
        String urlEvidenciaEntrega
) {
}
