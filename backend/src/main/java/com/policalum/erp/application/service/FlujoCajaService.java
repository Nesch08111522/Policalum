package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.response.FlujoCajaRealResponseDTO;
import com.policalum.erp.application.dto.response.FlujoDevengadoResponseDTO;
import com.policalum.erp.application.dto.response.ProyeccionFlujoResponseDTO;
import java.time.LocalDate;
import java.util.UUID;

/** Flujo de caja real, vista devengada y proyección (Módulo 6). Solo ADMIN. */
public interface FlujoCajaService {

    /**
     * Dinero que entró y salió en [desde, hasta] (fechas inclusivas; sin fechas: mes en curso),
     * separado en efectivo y digital. Sin sedeId: consolidado + desglose por sede.
     */
    FlujoCajaRealResponseDTO flujoReal(UUID sedeId, LocalDate desde, LocalDate hasta);

    /** Utilidad neta del periodo (devengado), consolidada y por sede. */
    FlujoDevengadoResponseDTO devengado(UUID sedeId, LocalDate desde, LocalDate hasta);

    /**
     * Proyección de los próximos meses (1 a 12, por defecto 3). El primer mes es el mes en
     * curso, prorrateado a los días que faltan.
     */
    ProyeccionFlujoResponseDTO proyeccion(UUID sedeId, Integer meses);
}
