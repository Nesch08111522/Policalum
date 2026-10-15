package com.policalum.erp.application.dto.request;

import java.math.BigDecimal;

/**
 * Datos de la entrega que acompañan a la foto (multipart). Se validan en el servicio
 * porque llegan como campos de formulario, no como JSON.
 *
 * @param recibidoPorNombre    obligatorio
 * @param recibidoPorDocumento opcional (DNI 8 dígitos o CE/pasaporte 9-12 alfanuméricos)
 * @param latitud              opcional, -90 a 90 (si se envía, también la longitud)
 * @param longitud             opcional, -180 a 180
 */
public record EvidenciaEntregaRequestDTO(
        String recibidoPorNombre,
        String recibidoPorDocumento,
        BigDecimal latitud,
        BigDecimal longitud,
        String observaciones
) { }
