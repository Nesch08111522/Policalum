package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.AperturaCajaRequestDTO;
import com.policalum.erp.application.dto.request.CierreCajaRequestDTO;
import com.policalum.erp.application.dto.response.ResumenCierreCajaDTO;
import com.policalum.erp.application.dto.response.TurnoCajaResponseDTO;
import java.util.UUID;

public interface TurnoCajaService {
    TurnoCajaResponseDTO abrir(AperturaCajaRequestDTO request);
    ResumenCierreCajaDTO cerrar(UUID turnoId, CierreCajaRequestDTO request);
    ResumenCierreCajaDTO resumen(UUID turnoId);
}
