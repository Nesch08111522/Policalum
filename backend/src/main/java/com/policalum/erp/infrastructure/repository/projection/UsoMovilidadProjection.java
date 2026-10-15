package com.policalum.erp.infrastructure.repository.projection;

import java.util.UUID;

/** Módulo 16: despachos de un vehículo por estado en un rango. */
public interface UsoMovilidadProjection {
    UUID getMovilidadId();
    String getPlaca();
    String getTipo();
    String getConductorNombre();
    String getConductorApellido();
    Boolean getActivo();
    Long getDespachos();
    Long getEntregados();
    Long getFallidos();
    Long getPendientes();
    Long getIntentosFallidos();
}
