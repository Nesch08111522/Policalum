package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.ClienteRequestDTO;
import com.policalum.erp.application.dto.response.ClienteResponseDTO;
import com.policalum.erp.domain.model.Cliente;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    @Mapping(target = "activo", defaultExpression = "java(Boolean.TRUE)")
    ClienteResponseDTO toResponseDto(Cliente cliente);

    Cliente toEntity(ClienteRequestDTO clienteRequestDTO);

    void updateEntityFromDto(ClienteRequestDTO dto, @MappingTarget Cliente entity);
}
