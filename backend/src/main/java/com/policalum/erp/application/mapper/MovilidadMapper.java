package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.request.MovilidadRequestDTO;
import com.policalum.erp.application.dto.response.MovilidadListadoDTO;
import com.policalum.erp.application.dto.response.MovilidadResponseDTO;
import com.policalum.erp.domain.model.Movilidad;
import com.policalum.erp.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** sede, conductor, activo y la placa normalizada los asigna MovilidadServiceImpl. */
@Mapper(componentModel = "spring")
public interface MovilidadMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "placa", ignore = true)
    @Mapping(target = "tipo", expression = "java(dto.tipo() != null ? dto.tipo().name() : null)")
    @Mapping(target = "conductor", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Movilidad toEntity(MovilidadRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "placa", ignore = true)
    @Mapping(target = "tipo", expression = "java(dto.tipo() != null ? dto.tipo().name() : entity.getTipo())")
    @Mapping(target = "conductor", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(MovilidadRequestDTO dto, @MappingTarget Movilidad entity);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "placa", source = "entity.placa")
    @Mapping(target = "tipo", source = "entity.tipo")
    @Mapping(target = "marca", source = "entity.marca")
    @Mapping(target = "modelo", source = "entity.modelo")
    @Mapping(target = "sedeId", source = "entity.sede.id")
    @Mapping(target = "sedeNombre", source = "entity.sede.nombre")
    @Mapping(target = "conductorId", source = "entity.conductor.id")
    @Mapping(target = "conductorNombre", expression = "java(nombreCompleto(entity.getConductor()))")
    @Mapping(target = "activo", source = "entity.activo")
    @Mapping(target = "ordenesActivas", source = "ordenesActivas")
    @Mapping(target = "createdAt", source = "entity.createdAt")
    MovilidadResponseDTO toResponseDto(Movilidad entity, long ordenesActivas);

    @Mapping(target = "sedeNombre", source = "sede.nombre")
    @Mapping(target = "conductorNombre", expression = "java(nombreCompleto(entity.getConductor()))")
    MovilidadListadoDTO toListadoDto(Movilidad entity);

    default String nombreCompleto(User u) {
        if (u == null) return null;
        return ((u.getNombre() != null ? u.getNombre() : "") + " " + (u.getApellido() != null ? u.getApellido() : "")).trim();
    }
}
