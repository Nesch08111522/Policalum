package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Caja y bancos estimados a hoy. total suma solo las cuentas con saldo registrado. */
public record DisponibleCalculadoDTO(BigDecimal total, List<SaldoCuentaEstimadoDTO> cuentas, List<String> advertencias) { }
