package com.policalum.erp.application.dto.request;

import com.policalum.erp.domain.enums.TipoMarcacion;
import java.time.LocalDateTime;
import java.util.UUID;

public record RrhhAsistenciaRequestDTO(
        UUID empleadoId,              // Quién es el empleado
        LocalDateTime fechaHora,       // Qué hora se le va a registrar manualmente
        TipoMarcacion tipoMarcacion,   // ENTRADA, SALIDA, etc.
        String dispositivoInfo,        // Ej: "Registro Manual por Admin"
        UUID turnoCajaId               // Opcional, si aplica
) {}