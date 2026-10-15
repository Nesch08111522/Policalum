package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.CategoriaRequestDTO;
import com.policalum.erp.application.dto.response.CategoriaResponseDTO;
import com.policalum.erp.domain.model.Categoria;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {
    CategoriaResponseDTO toResponseDto(Categoria categoria);

    Categoria toEntity(CategoriaRequestDTO categoriaRequestDTO);

    void updateEntityFromDto(CategoriaRequestDTO dto, @MappingTarget Categoria entity);
}
