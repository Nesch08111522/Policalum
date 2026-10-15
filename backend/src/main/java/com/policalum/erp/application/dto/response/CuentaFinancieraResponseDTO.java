package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.TipoCuentaFinanciera;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CuentaFinancieraResponseDTO(
        UUID id,
        String nombre,
        TipoCuentaFinanciera tipo,
        String entidad,
        String ultimosDigitos,
        UUID sedeId,
        String sedeNombre,
        List<MetodoPago> metodosPago,
        Boolean activa,
        BigDecimal ultimoSaldo,
        LocalDateTime ultimoCorte,
        LocalDateTime createdAt) { }
