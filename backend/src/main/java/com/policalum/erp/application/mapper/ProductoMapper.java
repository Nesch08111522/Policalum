package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.ProductoRequestDTO;
import com.policalum.erp.application.dto.response.ProductoResponseDTO;
import com.policalum.erp.domain.model.Producto;

@Mapper(componentModel = "spring")
public interface ProductoMapper {
    ProductoResponseDTO toResponseDto(Producto producto);

    Producto toEntity(ProductoRequestDTO productoRequestDTO);

    void updateEntityFromDto(ProductoRequestDTO dto, @MappingTarget Producto entity);
}
