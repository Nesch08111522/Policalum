package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.ServicioRequestDTO;
import com.policalum.erp.application.dto.response.ServicioResponseDTO;
import com.policalum.erp.domain.model.Servicio;

@Mapper(componentModel = "spring")
public interface ServicioMapper {
    @Mapping(target = "colaboradores", ignore = true)
    ServicioResponseDTO toResponseDto(Servicio servicio);

    @Mapping(target = "colaboradores", ignore = true)
    Servicio toEntity(ServicioRequestDTO servicioRequestDTO);

    @Mapping(target = "colaboradores", ignore = true)
    void updateEntityFromDto(ServicioRequestDTO dto, @MappingTarget Servicio entity);
}
