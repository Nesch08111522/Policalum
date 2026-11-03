package com.policalum.erp.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResumenChatDTO(
        UUID id,
        String nombre,
        String correo,
        String telefono,
        String empresa,
        String ultimoTexto,
        LocalDateTime ultimoMensajeAt,
        int noLeidos,
        boolean clienteEscribiendo,
        String estado,
        UUID clienteId
) {}
