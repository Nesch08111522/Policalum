package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Size;

public record GestionarMensajeRequestDTO(@Size(max = 500) String nota) {}
