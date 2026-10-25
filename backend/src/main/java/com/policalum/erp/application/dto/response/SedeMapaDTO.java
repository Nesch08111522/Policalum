package com.policalum.erp.application.dto.response;

import java.util.UUID;

/** Punto del mapa nacional. Las coordenadas pueden venir de la sede o del gazetter local. */
public record SedeMapaDTO(
        UUID id,
        String nombre,
        String ciudad,
        String direccion,
        Double latitud,
        Double longitud,
        String imagen,
        boolean ubicada
) {}
