package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.response.EvolucionMensualResponseDTO;
import com.policalum.erp.application.dto.response.KpisFinancierosResponseDTO;
import com.policalum.erp.application.dto.response.PatrimonioResponseDTO;
import java.time.LocalDate;
import java.util.UUID;

/** Dashboards financieros (Módulo 6). Solo ADMIN. */
public interface DashboardFinancieroService {

    /** Activos, pasivos y patrimonio neto a hoy + separación dueño/empresa del año. */
    PatrimonioResponseDTO patrimonio(UUID sedeId);

    /** Indicadores del periodo (sin fechas: mes en curso) comparados con el periodo anterior. */
    KpisFinancierosResponseDTO kpis(UUID sedeId, LocalDate desde, LocalDate hasta);

    /** Serie mensual de los últimos meses (1 a 24, por defecto 12), incluido el mes en curso. */
    EvolucionMensualResponseDTO evolucionMensual(UUID sedeId, Integer meses);
}
