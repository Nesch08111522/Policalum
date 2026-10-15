package com.policalum.erp.infrastructure.repository.projection;

import java.util.UUID;

public interface DespachosPorRepartidorProjection {
    UUID getResponsableId();
    String getNombre();
    String getApellido();
    Long getPendientes();
    Long getEnCamino();
}
