package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.request.TurnoTrabajoRequestDTO;
import com.policalum.erp.application.dto.response.TurnoTrabajoResponseDTO;
import com.policalum.erp.domain.model.RrhhTurnoTrabajo;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.util.HorarioUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** user, sede, diasSemana (normalizados), tolerancia y activo los asigna TurnoTrabajoServiceImpl. */
@Mapper(componentModel = "spring")
public interface TurnoTrabajoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "diasSemana", ignore = true)
    @Mapping(target = "toleranciaMinutos", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RrhhTurnoTrabajo toEntity(TurnoTrabajoRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "diasSemana", ignore = true)
    @Mapping(target = "toleranciaMinutos", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(TurnoTrabajoRequestDTO dto, @MappingTarget RrhhTurnoTrabajo entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "empleadoNombre", expression = "java(nombreCompleto(entity.getUser()))")
    @Mapping(target = "sedeId", source = "sede.id")
    @Mapping(target = "sedeNombre", source = "sede.nombre")
    @Mapping(target = "nocturno", expression = "java(esNocturno(entity.getHoraInicio(), entity.getHoraFin()))")
    @Mapping(target = "duracionHoras", expression = "java(duracionHoras(entity.getHoraInicio(), entity.getHoraFin()))")
    @Mapping(target = "horasSemanales", expression = "java(horasSemanales(entity))")
    @Mapping(target = "toleranciaMinutos", expression = "java(entity.getToleranciaSegura())")
    TurnoTrabajoResponseDTO toResponseDto(RrhhTurnoTrabajo entity);

    default String nombreCompleto(User user) {
        if (user == null) return null;
        return user.getNombre() + " " + user.getApellido();
    }

    default boolean esNocturno(LocalTime inicio, LocalTime fin) {
        return inicio != null && fin != null && fin.isBefore(inicio);
    }

    default BigDecimal duracionHoras(LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null) return null;
        return BigDecimal.valueOf(HorarioUtil.duracionMinutos(inicio, fin)).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    default BigDecimal horasSemanales(RrhhTurnoTrabajo t) {
        BigDecimal horas = duracionHoras(t.getHoraInicio(), t.getHoraFin());
        if (horas == null || t.getDiasSemana() == null) return null;
        return horas.multiply(BigDecimal.valueOf(t.getDiasSemana().size()));
    }
}
