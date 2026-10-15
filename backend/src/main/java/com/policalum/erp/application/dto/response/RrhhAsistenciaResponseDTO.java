    package com.policalum.erp.application.dto.response;

    import com.policalum.erp.domain.enums.TipoMarcacion;
    import java.time.LocalDateTime;
    import java.util.UUID;

    public record RrhhAsistenciaResponseDTO(
            UUID id,
            UUID empleadoId,
            String empleadoNombreCompleto,
            LocalDateTime fechaHora,
            TipoMarcacion tipoMarcacion,
            String dispositivoInfo,
            UUID turnoCajaId
    ) {}