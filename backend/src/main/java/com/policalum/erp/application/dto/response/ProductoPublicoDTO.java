package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Ficha de catálogo para el portal. Sin costo, proveedor ni datos internos. */
public record ProductoPublicoDTO(
        UUID id,
        String nombre,
        String descripcion,
        String codigoSku,
        String unidad,
        BigDecimal precioPublico,
        boolean disponible,
        boolean destacado,
        boolean esFraccionable,
        UUID categoriaId,
        String categoriaNombre,
        String imagenUrl,
        List<FraccionPublicaDTO> fracciones
) {}
