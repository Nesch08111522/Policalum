package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.VentaRequestDTO;
import com.policalum.erp.application.dto.response.VentaResponseDTO;
import com.policalum.erp.domain.model.Venta;

@Mapper(componentModel = "spring")
public interface VentaMapper {
    VentaResponseDTO toResponseDto(Venta venta);

    Venta toEntity(VentaRequestDTO ventaRequestDTO);

    void updateEntityFromDto(VentaRequestDTO dto, @MappingTarget Venta entity);
}
