package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapping;

import com.policalum.erp.application.dto.request.CatalogoFraccionRequestDTO;
import com.policalum.erp.application.dto.response.CatalogoFraccionResponseDTO;
import com.policalum.erp.domain.model.CatalogoFraccion;

@Mapper(componentModel = "spring")
public interface CatalogoFraccionMapper {
    CatalogoFraccionResponseDTO toResponseDto(CatalogoFraccion entity);

    @Mapping(target = "producto", ignore = true)
    CatalogoFraccion toEntity(CatalogoFraccionRequestDTO requestDTO);

    @Mapping(target = "producto", ignore = true)
    void updateEntityFromDto(CatalogoFraccionRequestDTO dto, @MappingTarget CatalogoFraccion entity);
}
