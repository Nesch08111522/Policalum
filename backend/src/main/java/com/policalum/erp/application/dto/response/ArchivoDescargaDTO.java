package com.policalum.erp.application.dto.response;

import org.springframework.core.io.Resource;

/** Archivo listo para devolver en la respuesta HTTP con su nombre y tipo originales. */
public record ArchivoDescargaDTO(Resource resource, String nombreOriginal, String contentType) { }
