package com.policalum.erp.application.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Arqueos de caja por turno (Módulo 12): resumen total + página de turnos (más recientes primero). */
public record ArqueosReporteDTO(
        LocalDate desde,
        LocalDate hasta,
        UUID sedeId,
        UUID usuarioId,
        ResumenArqueosDTO resumen,
        List<ArqueoTurnoDTO> turnos,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas) { }
