package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.DetalleVentaRequestDTO;
import com.policalum.erp.application.dto.response.DetalleVentaResponseDTO;
import com.policalum.erp.domain.model.DetalleVenta;

@Mapper(componentModel = "spring")
public interface DetalleVentaMapper {
    DetalleVentaResponseDTO toResponseDto(DetalleVenta entity);

    DetalleVenta toEntity(DetalleVentaRequestDTO requestDTO);

    void updateEntityFromDto(DetalleVentaRequestDTO dto, @MappingTarget DetalleVenta entity);
}
