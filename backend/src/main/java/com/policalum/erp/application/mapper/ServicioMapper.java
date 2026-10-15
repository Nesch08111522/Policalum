package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.ServicioRequestDTO;
import com.policalum.erp.application.dto.response.ServicioResponseDTO;
import com.policalum.erp.domain.model.Servicio;

@Mapper(componentModel = "spring")
public interface ServicioMapper {
    ServicioResponseDTO toResponseDto(Servicio servicio);

    Servicio toEntity(ServicioRequestDTO servicioRequestDTO);

    void updateEntityFromDto(ServicioRequestDTO dto, @MappingTarget Servicio entity);
}
