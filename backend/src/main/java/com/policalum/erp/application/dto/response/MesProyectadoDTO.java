package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;

/** Un mes de la proyección. El primero es el mes en curso (parcial: lo que falta del mes). */
public record MesProyectadoDTO(
        String periodo,
        boolean parcial,
        BigDecimal fraccionDelMes,
        BigDecimal ingresosEstimados,
        MontoProyectadoDTO pagosProveedores,
        MontoProyectadoDTO pagosNominas,
        MontoProyectadoDTO reembolsosClientes,
        MontoProyectadoDTO otrosPagos,
        BigDecimal totalEgresos,
        BigDecimal flujoOperativo,
        BigDecimal retirosEstimados,
        BigDecimal flujoNeto,
        BigDecimal saldoFinalEstimado) { }
