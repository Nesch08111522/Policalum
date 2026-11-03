package com.policalum.erp.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ContinuarChatRequest(
        @NotNull UUID referencia,
        @Email @Size(max = 150) String correo,
        @Size(max = 20) String telefono,
        @Size(max = 40) String dispositivo
) {}
