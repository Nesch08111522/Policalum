package com.policalum.erp.application.dto.response;

import java.util.UUID;

/** Coincidencia por el mismo navegador, el mismo correo o el mismo celular. No incluye la clave del chat. */
public record ReconocimientoChatDTO(
        boolean encontrada,
        boolean mismoDispositivo,
        String nombre,
        UUID referencia
) {}
