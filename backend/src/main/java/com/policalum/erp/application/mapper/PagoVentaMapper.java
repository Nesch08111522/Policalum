package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.PagoVentaRequestDTO;
import com.policalum.erp.application.dto.response.PagoVentaResponseDTO;
import com.policalum.erp.domain.model.PagoVenta;

@Mapper(componentModel = "spring")
public interface PagoVentaMapper {
    PagoVentaResponseDTO toResponseDto(PagoVenta entity);

    PagoVenta toEntity(PagoVentaRequestDTO requestDTO);

    void updateEntityFromDto(PagoVentaRequestDTO dto, @MappingTarget PagoVenta entity);
}
