package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.response.FacturacionElectronicaPanelDTO;
import java.util.UUID;

public interface FacturacionElectronicaService {
    FacturacionElectronicaPanelDTO panel(UUID sedeId);
}
