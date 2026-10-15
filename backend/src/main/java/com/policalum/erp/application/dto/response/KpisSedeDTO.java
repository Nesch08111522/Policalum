package com.policalum.erp.application.dto.response;

import java.util.UUID;

public record KpisSedeDTO(UUID sedeId, String sedeNombre, KpisFinancierosDTO kpis) { }
