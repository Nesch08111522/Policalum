package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cliente en el ranking (Módulo 12). Montos con IGV. devuelto = devoluciones aprobadas resueltas en el
 * rango. diasEntreCompras = días entre la primera y la última compra / (compras − 1); null con 1 compra.
 */
public record ClienteFrecuenteDTO(
        int posicion,
        UUID clienteId,
        String nombre,
        String tipoDocumento,
        String numeroDocumento,
        long compras,
        BigDecimal montoTotal,
        BigDecimal devuelto,
        BigDecimal montoNeto,
        BigDecimal ticketPromedio,
        LocalDateTime primeraCompra,
        LocalDateTime ultimaCompra,
        BigDecimal diasEntreCompras,
        long diasDesdeUltimaCompra) { }
