package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.ProveedorRequestDTO;
import com.policalum.erp.application.dto.response.ProveedorResponseDTO;
import com.policalum.erp.domain.model.Proveedor;

@Mapper(componentModel = "spring")
public interface ProveedorMapper {
    ProveedorResponseDTO toResponseDto(Proveedor proveedor);

    Proveedor toEntity(ProveedorRequestDTO proveedorRequestDTO);

    void updateEntityFromDto(ProveedorRequestDTO dto, @MappingTarget Proveedor entity);
}