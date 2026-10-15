package com.policalum.erp.application.dto.response;

import com.policalum.erp.domain.enums.EstadoDespacho;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Sin ruta de la foto: se descarga por GET /despachos/{id}/evidencia. */
public record OrdenDespachoResponseDTO(
        UUID id,
        String codigoSeguimiento,
        UUID ventaId,
        String ventaSerie,
        String clienteNombre,
        UUID sedeId,
        String sedeNombre,
        EstadoDespacho estado,
        boolean atrasado,
        String direccionEntrega,
        String referenciaEntrega,
        String contactoNombre,
        String contactoTelefono,
        LocalDate fechaProgramada,
        UUID responsableId,
        String responsableNombre,
        UUID movilidadId,
        String movilidadPlaca,
        LocalDateTime fechaAsignacion,
        LocalDateTime fechaSalida,
        LocalDateTime fechaEntrega,
        String recibidoPorNombre,
        String recibidoPorDocumento,
        BigDecimal latitudEntrega,
        BigDecimal longitudEntrega,
        boolean tieneEvidencia,
        Integer intentosFallidos,
        String motivoFallo,
        String observaciones,
        LocalDateTime createdAt
) { }
