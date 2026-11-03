package com.policalum.erp.application.dto.response;

public record AtencionPublicaDTO(
        boolean configurado,
        boolean atiendeHoy,
        boolean abiertoAhora,
        String dia,
        String desde,
        String hasta,
        String texto
) {}
