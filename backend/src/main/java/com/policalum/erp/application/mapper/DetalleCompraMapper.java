package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.DetalleCompraRequestDTO;
import com.policalum.erp.application.dto.response.DetalleCompraResponseDTO;
import com.policalum.erp.domain.model.DetalleCompra;

@Mapper(componentModel = "spring")
public interface DetalleCompraMapper {
    DetalleCompraResponseDTO toResponseDto(DetalleCompra entity);

    DetalleCompra toEntity(DetalleCompraRequestDTO requestDTO);

    void updateEntityFromDto(DetalleCompraRequestDTO dto, @MappingTarget DetalleCompra entity);
}
