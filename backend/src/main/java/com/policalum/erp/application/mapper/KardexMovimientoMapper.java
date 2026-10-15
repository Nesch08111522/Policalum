package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.KardexMovimientoRequestDTO;
import com.policalum.erp.application.dto.response.KardexMovimientoResponseDTO;
import com.policalum.erp.domain.model.KardexMovimiento;

@Mapper(componentModel = "spring")
public interface KardexMovimientoMapper {
    KardexMovimientoResponseDTO toResponseDto(KardexMovimiento entity);

    KardexMovimiento toEntity(KardexMovimientoRequestDTO requestDTO);

    void updateEntityFromDto(KardexMovimientoRequestDTO dto, @MappingTarget KardexMovimiento entity);

    KardexMovimientoResponseDTO toResponseDTO(KardexMovimiento guardado);
}
