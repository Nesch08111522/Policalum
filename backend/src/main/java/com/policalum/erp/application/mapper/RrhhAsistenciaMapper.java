package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.RrhhAsistenciaRequestDTO;
import com.policalum.erp.application.dto.response.RrhhAsistenciaResponseDTO;
import com.policalum.erp.domain.model.RrhhAsistencia;

@Mapper(componentModel = "spring")
public interface RrhhAsistenciaMapper {
    @Mapping(target = "empleadoId", source = "empleado.id")
    @Mapping(target = "empleadoNombreCompleto", expression = "java(entity.getEmpleado().getNombre() + \" \" + entity.getEmpleado().getApellido())")
    RrhhAsistenciaResponseDTO toResponseDto(RrhhAsistencia entity);

    RrhhAsistencia toEntity(RrhhAsistenciaRequestDTO requestDTO);

    void updateEntityFromDto(RrhhAsistenciaRequestDTO dto, @MappingTarget RrhhAsistencia entity);
}
