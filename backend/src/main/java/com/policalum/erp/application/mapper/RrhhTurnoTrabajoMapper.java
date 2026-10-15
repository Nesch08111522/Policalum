package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.RrhhTurnoTrabajoRequestDTO;
import com.policalum.erp.application.dto.response.RrhhTurnoTrabajoResponseDTO;
import com.policalum.erp.domain.model.RrhhTurnoTrabajo;

@Mapper(componentModel = "spring")
public interface RrhhTurnoTrabajoMapper {
    RrhhTurnoTrabajoResponseDTO toResponseDto(RrhhTurnoTrabajo entity);

    RrhhTurnoTrabajo toEntity(RrhhTurnoTrabajoRequestDTO requestDTO);

    void updateEntityFromDto(RrhhTurnoTrabajoRequestDTO dto, @MappingTarget RrhhTurnoTrabajo entity);
}
