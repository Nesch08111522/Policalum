package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.AlmacenRequestDTO;
import com.policalum.erp.application.dto.response.AlmacenResponseDTO;
import com.policalum.erp.domain.model.Almacen;

@Mapper(componentModel = "spring", uses = SedeMapper.class)
public interface AlmacenMapper {
    AlmacenResponseDTO toResponseDto(Almacen almacen);

    Almacen toEntity(AlmacenRequestDTO almacenRequestDTO);

    void updateEntityFromDto(AlmacenRequestDTO dto, @MappingTarget Almacen entity);
}
