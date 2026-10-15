// src/main/java/com/policalum/erp/application/dto/response/AsistenciaEstadoDTO.java
package com.policalum.erp.application.dto.response;

public record AsistenciaEstadoDTO(
        boolean yaMarcoEntrada,
        boolean yaMarcoSalida,
        String mensajeEntrada,
        String mensajeSalida
) {}