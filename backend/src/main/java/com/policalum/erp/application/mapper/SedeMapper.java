package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.SedeRequestDTO;
import com.policalum.erp.application.dto.response.SedeResponseDTO;
import com.policalum.erp.domain.model.Sede;

@Mapper(componentModel = "spring")
public interface SedeMapper {

    SedeResponseDTO toResponseDto(Sede sede);

    @Mapping(source = "nombre", target = "nombre")
    @Mapping(source = "pais", target = "pais")
    @Mapping(source = "ciudad", target = "ciudad")
    @Mapping(source = "direccion", target = "direccion")
    @Mapping(source = "celularSede", target = "celularSede")
    @Mapping(source = "moneda", target = "moneda")
    @Mapping(source = "activa", target = "activa")
    Sede toEntity(SedeRequestDTO sedeRequestDTO);

    @Mapping(source = "nombre", target = "nombre")
    @Mapping(source = "pais", target = "pais")
    @Mapping(source = "ciudad", target = "ciudad")
    @Mapping(source = "direccion", target = "direccion")
    @Mapping(source = "celularSede", target = "celularSede")
    @Mapping(source = "moneda", target = "moneda")
    @Mapping(source = "activa", target = "activa")
    void updateEntityFromDto(SedeRequestDTO dto, @MappingTarget Sede entity);
}