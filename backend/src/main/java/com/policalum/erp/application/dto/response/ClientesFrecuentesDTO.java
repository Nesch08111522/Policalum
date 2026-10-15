package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.CriterioRankingCliente;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Ranking de clientes (Módulo 12). Las ventas sin cliente (público general) se informan aparte. */
public record ClientesFrecuentesDTO(
        LocalDate desde,
        LocalDate hasta,
        UUID sedeId,
        CriterioRankingCliente criterio,
        int top,
        long clientesConCompras,
        long ventasSinCliente,
        BigDecimal montoSinCliente,
        List<ClienteFrecuenteDTO> items) { }
