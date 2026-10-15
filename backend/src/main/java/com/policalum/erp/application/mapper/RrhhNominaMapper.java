package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.RrhhNominaRequestDTO;
import com.policalum.erp.application.dto.response.RrhhNominaResponseDTO;
import com.policalum.erp.domain.model.RrhhNomina;

@Mapper(componentModel = "spring")
public interface RrhhNominaMapper {
    RrhhNominaResponseDTO toResponseDto(RrhhNomina entity);

    RrhhNomina toEntity(RrhhNominaRequestDTO requestDTO);

    void updateEntityFromDto(RrhhNominaRequestDTO dto, @MappingTarget RrhhNomina entity);
}
