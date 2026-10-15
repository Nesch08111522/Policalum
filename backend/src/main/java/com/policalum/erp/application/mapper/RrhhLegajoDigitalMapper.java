package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.RrhhLegajoDigitalRequestDTO;
import com.policalum.erp.application.dto.response.RrhhLegajoDigitalResponseDTO;
import com.policalum.erp.domain.model.RrhhLegajoDigital;

@Mapper(componentModel = "spring")
public interface RrhhLegajoDigitalMapper {
    RrhhLegajoDigitalResponseDTO toResponseDto(RrhhLegajoDigital entity);

    RrhhLegajoDigital toEntity(RrhhLegajoDigitalRequestDTO requestDTO);

    void updateEntityFromDto(RrhhLegajoDigitalRequestDTO dto, @MappingTarget RrhhLegajoDigital entity);
}
