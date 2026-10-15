package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Foto del patrimonio a hoy. Montos en PEN.
 *   activos  = disponible (caja y bancos estimados) + inventario a precio de compra
 *   pasivos  = CxP pendientes + nóminas por pagar + reembolsos por pagar
 *   patrimonioNeto = activos - pasivos
 * No hay cuentas por cobrar: todas las ventas se cobran al contado.
 */
public record PatrimonioResponseDTO(
        UUID sedeId,
        LocalDateTime calculadoEn,
        BigDecimal disponible,
        List<SaldoCuentaEstimadoDTO> cuentas,
        BigDecimal inventarioValorizado,
        long productosSinCosto,
        BigDecimal totalActivos,
        BigDecimal cuentasPorPagar,
        BigDecimal cuentasPorPagarVencidas,
        BigDecimal nominasPorPagar,
        BigDecimal reembolsosPorPagar,
        BigDecimal totalPasivos,
        BigDecimal patrimonioNeto,
        SeparacionPatrimonialDTO separacion,
        List<PatrimonioSedeDTO> porSede,
        List<String> advertencias) { }
