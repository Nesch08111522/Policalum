package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;

/**
 * tendencia    = promedio histórico (prorrateado si el mes es parcial)
 * comprometido = documentos ya existentes (CxP que vencen, nóminas y reembolsos por pagar)
 * estimado     = el mayor de los dos: nunca se proyecta menos de lo ya comprometido y no se
 *                suman ambos, porque la tendencia ya incluye ese tipo de pagos.
 */
public record MontoProyectadoDTO(BigDecimal tendencia, BigDecimal comprometido, BigDecimal estimado) { }
