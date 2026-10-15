package com.policalum.erp.application.dto.response;

import java.util.Map;

/**
 * Flujo de caja REAL (lo que entró y salió) de un periodo. Montos en PEN.
 *
 * flujoOperativo = cobrosVentas - (pagosProveedores + pagosNominas + reembolsosClientes + otrosPagos)
 * flujoNeto      = flujoOperativo - retirosSocios
 */
public record FlujoCajaResumenDTO(
        MontoPorMedioDTO cobrosVentas,
        Map<String, java.math.BigDecimal> cobrosPorMetodo,
        MontoPorMedioDTO pagosProveedores,
        MontoPorMedioDTO pagosNominas,
        MontoPorMedioDTO reembolsosClientes,
        MontoPorMedioDTO otrosPagos,
        MontoPorMedioDTO totalEgresosOperativos,
        MontoPorMedioDTO flujoOperativo,
        MontoPorMedioDTO retirosSocios,
        MontoPorMedioDTO flujoNeto) { }
